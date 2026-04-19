/**
 * 文件下载Hook
 * 职责：封装四阶段下载逻辑（apply → chunk → decrypt → finish）
 */

import { ref, reactive, computed } from 'vue';
import { ElMessage } from 'element-plus';
import request from '../utils/request';
import {
    unwrapAESKeyWithRSA,
    decryptChunk,
    importPrivateKey,
    loadPrivateKeyLocal
} from '../utils/crypto';
import {
    saveAsFile,
    verifyChunkIntegrity,
    chunkStorage,
    formatFileSize,
    calculateProgress
} from '../utils/fileUtils';
import type {
    DownloadTask,
    DownloadApplyDTO,
    DownloadApplyVO,
    ChunkTask,
    ChunkMeta
} from '../types/download';
import { useAuthStore } from '../stores/auth';

// 配置常量
const MAX_CONCURRENT = 3;           // 最大并发下载数
const CHUNK_RETRY_LIMIT = 3;        // 分片最大重试次数
const CHUNK_TIMEOUT = 30000;        // 分片下载超时（毫秒）

/**
 * 下载队列状态管理
 */
const downloadQueue = reactive<{
    tasks: DownloadTask[];
    activeCount: number;
    maxConcurrent: number;
}>({
    tasks: [],
    activeCount: 0,
    maxConcurrent: MAX_CONCURRENT
});

/**
 * 生成唯一ID
 */
const generateId = (): string => {
    return Date.now().toString(36) + Math.random().toString(36).substring(2);
};

/**
 * 创建下载任务
 */
const createTask = (fileId: string, fileName: string, fileSize: number): DownloadTask => {
    return {
        id: generateId(),
        fileId,
        fileName,
        fileSize,
        status: 'pending',
        progress: 0,
        chunks: [],
        createdAt: Date.now(),
        completedChunks: 0,
        totalChunks: 0,
        downloadedChunks: []
    };
};

/**
 * 从IndexedDB恢复任务
 */
const restoreTaskFromStorage = async (task: DownloadTask): Promise<void> => {
    try {
        const savedChunks = await chunkStorage.getFileChunks(task.fileId);
        if (savedChunks.size > 0) {
            // 更新分片状态
            task.chunks.forEach(chunk => {
                if (savedChunks.has(chunk.index)) {
                    chunk.status = 'completed';
                    chunk.data = savedChunks.get(chunk.index);
                }
            });

            // 更新已完成计数
            task.completedChunks = task.chunks.filter(c => c.status === 'completed').length;
            task.progress = calculateProgress(task.completedChunks, task.totalChunks);

            console.log(`从存储恢复任务 ${task.fileId}, 已恢复 ${task.completedChunks}/${task.totalChunks} 分片`);
        }
    } catch (error) {
        console.error('恢复任务失败:', error);
    }
};

/**
 * 下载单个分片
 */
const downloadChunk = async (
    task: DownloadTask,
    chunk: ChunkTask,
    sessionId: string
): Promise<ArrayBuffer | null> => {
    try {
        chunk.status = 'downloading';

        // 构建请求URL
        const url = `/api/v1/file/download/chunk?sessionId=${sessionId}&chunkIndex=${chunk.index}`;

        // 发起请求，设置为arraybuffer响应类型
        const response = await request.get(url, {
            responseType: 'arraybuffer',
            timeout: CHUNK_TIMEOUT
        });

        const data = response.data as ArrayBuffer;

        // 验证分片完整性
        const isValid = await verifyChunkIntegrity(data, chunk.sha256);
        if (!isValid) {
            throw new Error(`分片 ${chunk.index} 完整性校验失败`);
        }

        // 保存到IndexedDB
        await chunkStorage.saveChunk(task.fileId, chunk.index, data, chunk.sha256);

        chunk.status = 'completed';
        chunk.data = data;
        task.completedChunks++;
        task.progress = calculateProgress(task.completedChunks, task.totalChunks);

        return data;
    } catch (error) {
        console.error(`下载分片 ${chunk.index} 失败:`, error);
        chunk.retryCount++;

        if (chunk.retryCount >= CHUNK_RETRY_LIMIT) {
            chunk.status = 'failed';
            task.status = 'failed';
            task.error = `分片 ${chunk.index} 下载失败，已达最大重试次数`;
            ElMessage.error(task.error);
        } else {
            chunk.status = 'pending'; // 稍后重试
        }

        return null;
    }
};

/**
 * 并发下载所有分片
 */
const downloadAllChunks = async (task: DownloadTask): Promise<boolean> => {
    const { sessionId, wrappedAesKey, chunkMetas } = task;

    if (!sessionId || !wrappedAesKey || !chunkMetas) {
        task.status = 'failed';
        task.error = '下载会话信息不完整';
        return false;
    }

    // 初始化分片任务
    task.chunks = chunkMetas.map((meta) => ({
        index: meta.chunkIndex,
        size: meta.chunkSize,
        sha256: meta.cipherSha256,
        status: 'pending',
        retryCount: 0
    }));

    task.totalChunks = task.chunks.length;
    task.completedChunks = 0;

    // 从存储恢复已下载分片
    await restoreTaskFromStorage(task);

    // 如果所有分片已完成，直接返回成功
    if (task.completedChunks === task.totalChunks) {
        return true;
    }

    task.status = 'downloading';

    // 并发控制
    const pendingChunks = task.chunks.filter(c => c.status !== 'completed');
    let currentIndex = 0;
    let activeCount = 0;

    return new Promise((resolve) => {
        const processNext = async () => {
            // 检查是否完成或暂停
            if (task.status === 'paused' || task.status === 'cancelled') {
                return;
            }

            // 检查是否所有分片已完成
            const completed = task.chunks.filter(c => c.status === 'completed').length;
            if (completed === task.totalChunks) {
                task.status = 'decrypting';
                resolve(true);
                return;
            }

            // 启动新的下载
            while (activeCount < MAX_CONCURRENT && currentIndex < pendingChunks.length) {
                const chunk = pendingChunks[currentIndex];
                currentIndex++;
                activeCount++;

                downloadChunk(task, chunk, sessionId!).finally(() => {
                    activeCount--;
                    processNext();
                });
            }

            // 如果没有活跃任务且未完成，说明有失败分片
            if (activeCount === 0 && currentIndex >= pendingChunks.length) {
                const failed = task.chunks.some(c => c.status === 'failed');
                if (failed) {
                    task.status = 'failed';
                    resolve(false);
                }
            }
        };

        processNext();
    });
};

/**
 * 解密并合并文件
 */
const decryptAndMerge = async (task: DownloadTask, privateKeyPem: string): Promise<boolean> => {
    try {
        task.status = 'decrypting';

        // 导入RSA私钥
        const privateKey = await importPrivateKey(privateKeyPem);

        // 解密AES密钥
        if (!task.wrappedAesKey) {
            throw new Error('缺少封装的AES密钥');
        }
        const aesKey = await unwrapAESKeyWithRSA(task.wrappedAesKey, privateKey);

        // 获取所有分片数据（按索引排序）
        const sortedChunks = [...task.chunks].sort((a, b) => a.index - b.index);

        // 解密所有分片
        const decryptedBuffers: ArrayBuffer[] = [];

        for (let i = 0; i < sortedChunks.length; i++) {
            const chunk = sortedChunks[i];
            if (!chunk.data) {
                // 尝试从存储加载
                const storedData = await chunkStorage.getChunk(task.fileId, chunk.index);
                if (!storedData) {
                    throw new Error(`分片 ${chunk.index} 数据丢失`);
                }
                chunk.data = storedData;
            }

            // 解密分片
            const decrypted = await decryptChunk(chunk.data, aesKey);
            decryptedBuffers.push(decrypted);
            task.downloadedChunks.push(decrypted);

            // 更新进度
            task.progress = calculateProgress(i + 1, task.totalChunks);
        }

        // 保存解密后的文件
        saveAsFile(decryptedBuffers, task.fileName);

        return true;
    } catch (error) {
        console.error('解密文件失败:', error);
        task.status = 'failed';
        task.error = error instanceof Error ? error.message : '解密失败';
        ElMessage.error(task.error);
        return false;
    }
};

/**
 * 完成下载（调用finish接口）
 */
const finishDownload = async (task: DownloadTask): Promise<void> => {
    if (!task.sessionId) return;

    try {
        await request.post(`/api/v1/file/download/finish?sessionId=${task.sessionId}`);
        console.log('下载完成通知发送成功');

        // 清理存储的分片
        await chunkStorage.deleteFileChunks(task.fileId);
    } catch (error) {
        console.error('发送完成通知失败:', error);
        // 不影响前端下载结果，只记录错误
    }
};

/**
 * 使用文件下载
 */
export function useFileDownload() {
    const authStore = useAuthStore();

    // 获取当前用户的私钥
    const getPrivateKey = (): string | null => {
        const username = authStore.username || 'current';
        return loadPrivateKeyLocal(username);
    };

    /**
     * 开始下载文件
     * @param fileId 文件ID
     * @param extractCode 提取码（可选）
     */
    const startDownload = async (fileId: string, extractCode?: string): Promise<boolean> => {
        // 检查私钥
        const privateKey = getPrivateKey();
        if (!privateKey) {
            ElMessage.error('未找到本地私钥，请先在密钥管理页面生成密钥对');
            return false;
        }

        // 创建任务 - 使用文件ID作为临时名称
        const task = createTask(fileId, `文件_${fileId.substring(0, 8)}...`, 0);
        task.status = 'applying';
        downloadQueue.tasks.push(task);

        try {
            // 阶段1：申请下载
            const applyDTO: DownloadApplyDTO = { fileId };
            if (extractCode) applyDTO.extractCode = extractCode;

            console.log('发送下载申请:', applyDTO);
            const applyRes = await request.post('/api/v1/file/download/apply', applyDTO);
            console.log('下载申请响应:', applyRes.data);

            const applyData = applyRes.data.data as DownloadApplyVO;

            // 更新任务信息
            task.fileName = applyData.fileName;  // 这里会更新为真实文件名
            task.fileSize = applyData.fileSize;
            task.sessionId = applyData.downloadSessionId;
            task.wrappedAesKey = applyData.wrappedAesKey;
            task.chunkMetas = applyData.chunkMetaList;

            console.log('下载申请成功', applyData);

            // 阶段2：并发下载分片
            const downloadSuccess = await downloadAllChunks(task);
            if (!downloadSuccess) {
                return false;
            }

            // 阶段3：解密合并
            const decryptSuccess = await decryptAndMerge(task, privateKey);
            if (!decryptSuccess) {
                return false;
            }

            // 阶段4：完成通知
            task.status = 'completed';
            task.progress = 100;
            await finishDownload(task);

            ElMessage.success(`文件下载完成: ${task.fileName}`);
            return true;

        } catch (error: any) {
            console.error('下载失败:', error);
            task.status = 'failed';

            // 更友好的错误提示
            if (error.response) {
                const status = error.response.status;
                const message = error.response.data?.message || error.message;

                if (status === 401) {
                    task.error = '未授权，请重新登录';
                } else if (status === 403) {
                    task.error = '没有权限下载此文件';
                } else if (status === 404) {
                    task.error = '文件不存在';
                } else if (status === 400 && message.includes('提取码')) {
                    task.error = '提取码错误';
                } else {
                    task.error = message || `下载失败 (${status})`;
                }
            } else if (error.request) {
                task.error = '网络错误，无法连接到服务器';
            } else {
                task.error = error?.message || '下载失败';
            }

            ElMessage.error(task.error);
            return false;
        }
    };

    /**
     * 暂停下载任务
     * @param taskId 任务ID
     */
    const pauseTask = (taskId: string): void => {
        const task = downloadQueue.tasks.find(t => t.id === taskId);
        if (task && (task.status === 'downloading' || task.status === 'pending')) {
            task.status = 'paused';
            ElMessage.info(`已暂停: ${task.fileName}`);
        }
    };

    /**
     * 恢复下载任务
     * @param taskId 任务ID
     */
    const resumeTask = async (taskId: string): Promise<void> => {
        const task = downloadQueue.tasks.find(t => t.id === taskId);
        if (!task || task.status !== 'paused') return;

        const privateKey = getPrivateKey();
        if (!privateKey) {
            ElMessage.error('未找到本地私钥');
            return;
        }

        task.status = 'downloading';

        // 继续下载未完成的分片
        const pendingChunks = task.chunks.filter(c => c.status !== 'completed');
        if (pendingChunks.length === 0) {
            // 所有分片已完成，进入解密阶段
            task.status = 'decrypting';
            const decryptSuccess = await decryptAndMerge(task, privateKey);
            if (decryptSuccess) {
                task.status = 'completed';
                await finishDownload(task);
            }
        } else {
            // 继续下载
            const downloadSuccess = await downloadAllChunks(task);
            if (downloadSuccess) {
                const decryptSuccess = await decryptAndMerge(task, privateKey);
                if (decryptSuccess) {
                    task.status = 'completed';
                    await finishDownload(task);
                }
            }
        }
    };

    /**
     * 取消下载任务
     * @param taskId 任务ID
     */
    const cancelTask = async (taskId: string): Promise<void> => {
        const task = downloadQueue.tasks.find(t => t.id === taskId);
        if (!task) return;

        task.status = 'cancelled';

        // 清理存储的分片
        await chunkStorage.deleteFileChunks(task.fileId);

        // 从队列中移除
        const index = downloadQueue.tasks.findIndex(t => t.id === taskId);
        if (index !== -1) {
            downloadQueue.tasks.splice(index, 1);
        }

        ElMessage.info(`已取消: ${task.fileName}`);
    };

    /**
     * 重试失败的任务
     * @param taskId 任务ID
     */
    const retryTask = async (taskId: string): Promise<void> => {
        const task = downloadQueue.tasks.find(t => t.id === taskId);
        if (!task || task.status !== 'failed') return;

        // 重置失败分片
        task.chunks.forEach(chunk => {
            if (chunk.status === 'failed') {
                chunk.status = 'pending';
                chunk.retryCount = 0;
            }
        });

        task.status = 'pending';
        task.error = undefined;

        // 重新开始
        await resumeTask(taskId);
    };

    /**
     * 清除已完成的任务
     */
    const clearCompletedTasks = (): void => {
        downloadQueue.tasks = downloadQueue.tasks.filter(t => t.status !== 'completed');
    };

    /**
     * 获取所有任务
     */
    const tasks = computed(() => downloadQueue.tasks);

    /**
     * 获取活跃任务数
     */
    const activeCount = computed(() =>
        downloadQueue.tasks.filter(t =>
            t.status === 'downloading' || t.status === 'decrypting' || t.status === 'applying'
        ).length
    );

    /**
     * 获取队列统计信息
     */
    const queueStats = computed(() => {
        const all = downloadQueue.tasks.length;
        const active = activeCount.value;
        const paused = downloadQueue.tasks.filter(t => t.status === 'paused').length;
        const completed = downloadQueue.tasks.filter(t => t.status === 'completed').length;
        const failed = downloadQueue.tasks.filter(t => t.status === 'failed').length;

        return { all, active, paused, completed, failed };
    });

    return {
        tasks,
        activeCount,
        queueStats,
        startDownload,
        pauseTask,
        resumeTask,
        cancelTask,
        retryTask,
        clearCompletedTasks
    };
}