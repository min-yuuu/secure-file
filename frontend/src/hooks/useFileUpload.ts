// src/hooks/useFileUpload.ts
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
// 适配你 crypto.js 里的函数名
import {
    encryptChunk as encryptAES,
    sha256Hex as calculateSHA256,
    wrapAESKeyWithRSA,
    generateAESKey,
    exportAESKey,
    importAESKey,
    importPublicKey
} from '../utils/crypto'
import { useAuthStore } from '../stores/auth'
import type {
    UploadInitRequest,
    ChunkInfo,
    Recipient
} from '../types/upload'

// 配置常量
const CHUNK_SIZE = 1024 * 1024; // 分片大小 1MB
const MAX_CONCURRENCY = 3;      // 最大并发上传数
const MAX_RETRY = 3;            // 分片最大重试次数

export function useFileUpload() {
    const authStore = useAuthStore();

    // 上传文件基础信息
    const file = ref<File | null>(null);
    const fileId = ref('');
    const extractCode = ref('');
    const expiryHours = ref(24);
    const downloadLimit = ref(-1);
    const burnAfterReading = ref(false);
    const recipients = ref<Recipient[]>([]);

    // 分片相关状态
    const chunks = ref<ChunkInfo[]>([]);
    const aesKey = ref<ArrayBuffer | null>(null);  // 保存为 ArrayBuffer
    const aesKeyObj = ref<CryptoKey | null>(null); // 新增：保存 CryptoKey 对象
    const isUploading = ref(false);
    const isPaused = ref(false);

    // 上传进度（百分比）
    const progress = computed(() => {
        if (chunks.value.length === 0) return 0;
        const successCount = chunks.value.filter(c => c.status === 'success').length;
        return Math.round((successCount / chunks.value.length) * 100);
    });

    // 从token解析用户ID
    const getCurrentUserId = (): string | null => {
        const token = authStore.token;
        if (!token) return null;
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            console.log('token payload:', payload);
            return payload.userId || payload.userid || payload.sub || null;
        } catch (error) {
            console.error('解析用户ID失败:', error);
            return null;
        }
    };

    // 生成分片信息
    const createChunks = (file: File, uploadedChunks: number[]) => {
        console.log('创建分片，已上传分片:', uploadedChunks);

        const chunkList: ChunkInfo[] = [];
        let index = 0;
        let start = 0;

        while (start < file.size) {
            const end = Math.min(start + CHUNK_SIZE, file.size);
            const chunkData = file.slice(start, end);

            chunkList.push({
                index,
                size: end - start,
                data: chunkData,
                status: uploadedChunks.includes(index) ? 'success' : 'waiting',
                retryCount: 0
            });

            start = end;
            index++;
        }

        chunks.value = chunkList;
        console.log('分片创建完成，总数:', chunkList.length);
    };

    // 1. 初始化上传（对接后端 /api/v1/file/upload/init）
    const initUpload = async (): Promise<boolean> => {
        console.log('=== 开始初始化上传 ===')
        console.log('文件信息:', file.value)

        if (!file.value) {
            console.log('没有文件，初始化失败')
            ElMessage.error('请先选择文件');
            return false;
        }

        // 计算总分片数和文件明文 SHA256
        const totalChunks = Math.ceil(file.value.size / CHUNK_SIZE);
        console.log('总分片数:', totalChunks)

        console.log('开始计算文件SHA256...')
        const plainSha256 = await calculateSHA256(file.value);
        console.log('文件SHA256计算完成:', plainSha256)

        // 构造初始化请求参数
        const initParams: UploadInitRequest = {
            fileName: file.value.name,
            fileSize: file.value.size,
            chunkCount: totalChunks,
            extractCode: extractCode.value || undefined,
            expiryHours: expiryHours.value,
            downloadLimit: downloadLimit.value,
            burnAfterReading: burnAfterReading.value,
            contentType: file.value.type || 'application/octet-stream',
            plainSha256: plainSha256
        };

        console.log('发送初始化请求，参数:', initParams)

        try {
            // 调用初始化接口
            const res = await request.post('/api/v1/file/upload/init', initParams);
            console.log('初始化响应:', res.data)

            // 后端返回的数据在 data 字段里
            const responseData = res.data.data;
            console.log('响应数据:', responseData)

            // 获取 fileId
            fileId.value = responseData.fileId;
            console.log('获取到fileId:', fileId.value)

            if (!fileId.value) {
                console.error('fileId为空，响应数据:', responseData);
                ElMessage.error('初始化失败：未获取到文件ID');
                return false;
            }

            // 生成分片信息（识别已上传分片，支持断点续传）
            // 使用 uploadedChunkIndexList 字段
            const uploadedChunks = responseData.uploadedChunkIndexList || [];
            console.log('已上传分片:', uploadedChunks)

            createChunks(file.value, uploadedChunks);
            console.log('分片信息创建完成，分片数量:', chunks.value.length)

            // 生成AES密钥 - 同时保存 ArrayBuffer 和 CryptoKey
            console.log('开始生成AES密钥...')
            const keyObj = await generateAESKey();
            aesKeyObj.value = keyObj; // 保存 CryptoKey 对象
            aesKey.value = await exportAESKey(keyObj); // 保存 ArrayBuffer
            console.log('AES密钥生成完成')

            return true;
        } catch (err) {
            console.error('初始化失败详情：', err);
            ElMessage.error('初始化上传失败');
            return false;
        }
    };

    // 2. 分片加密上传（核心逻辑，含并发控制）
    const uploadChunks = async (): Promise<void> => {
        if (!fileId.value || !aesKeyObj.value) {
            ElMessage.error('请先完成上传初始化');
            return;
        }

        isUploading.value = true;
        isPaused.value = false;
        let activeCount = 0;
        let currentIndex = 0;

        // 并发控制核心函数
        const uploadNextChunk = () => {
            // 暂停/全部分片处理完成，终止流程
            if (isPaused.value || currentIndex >= chunks.value.length) {
                if (activeCount === 0) {
                    isUploading.value = false;
                    // 检查所有分片是否上传成功
                    const allSuccess = chunks.value.every(c => c.status === 'success');
                    if (allSuccess) {
                        ElMessage.success('所有分片上传完成，正在完成最终提交');
                        completeUpload(); // 自动触发完成上传
                    } else {
                        ElMessage.warning('部分分片上传失败，请点击「继续上传」重试');
                    }
                }
                return;
            }

            // 控制并发数，循环上传分片
            while (activeCount < MAX_CONCURRENCY && currentIndex < chunks.value.length) {
                const chunk = chunks.value[currentIndex];

                // 跳过已成功/上传中的分片
                if (chunk.status === 'success' || chunk.status === 'uploading') {
                    currentIndex++;
                    continue;
                }

                activeCount++;
                chunk.status = 'uploading';

                // 上传单个分片，完成后释放并发数
                uploadSingleChunk(chunk)
                    .then(() => {
                        activeCount--;
                        uploadNextChunk();
                    })
                    .catch(() => {
                        activeCount--;
                        uploadNextChunk();
                    });

                currentIndex++;
            }
        };

        uploadNextChunk();
    };

    // 上传单个分片
    const uploadSingleChunk = async (chunk: ChunkInfo): Promise<void> => {
        try {
            // 使用保存的 AES 密钥对象
            const aesKeyObjValue = aesKeyObj.value;
            if (!aesKeyObjValue) {
                throw new Error('AES密钥对象不存在');
            }

            // 将 Blob 转换为 ArrayBuffer 进行加密
            const chunkArrayBuffer = await chunk.data.arrayBuffer();

            // encryptAES 返回的是 Uint8Array（从 crypto.js 可知）
            const encryptedUint8Array = await encryptAES(chunkArrayBuffer, aesKeyObjValue);

            // 计算密文 SHA256 - 直接传递 Uint8Array
            const cipherSha256 = await calculateSHA256(encryptedUint8Array);

            // 构造 FormData 上传分片 - 直接使用 Uint8Array
            const formData = new FormData();
            formData.append('blob', new Blob([encryptedUint8Array]));

            console.log(`上传分片 ${chunk.index}, 大小: ${encryptedUint8Array.length} 字节`);

            // 调用分片上传接口
            await request.post('/api/v1/file/upload/chunk', formData, {
                params: {
                    fileId: fileId.value,
                    chunkIndex: chunk.index,
                    cipherSha256: cipherSha256
                },
                headers: { 'Content-Type': 'multipart/form-data' },
                timeout: 30000
            });

            chunk.status = 'success';
            console.log(`分片 ${chunk.index} 上传成功`);
        } catch (err) {
            console.error(`分片 ${chunk.index} 上传失败:`, err);
            chunk.retryCount++;
            if (chunk.retryCount < MAX_RETRY) {
                chunk.status = 'waiting';
                console.log(`分片 ${chunk.index} 第 ${chunk.retryCount} 次重试`);
                await new Promise(r => setTimeout(r, 1000));
                await uploadSingleChunk(chunk);
            } else {
                chunk.status = 'failed';
                ElMessage.error(`分片 ${chunk.index} 上传失败，已达最大重试次数`);
            }
        }
    };

    // 3. 完成上传（对接后端 /api/v1/file/upload/complete）
    const completeUpload = async (): Promise<boolean> => {
        if (!fileId.value || !file.value || !aesKeyObj.value) {
            ElMessage.error('上传信息不完整，无法完成提交');
            return false;
        }

        // 检查所有分片是否上传成功
        const allSuccess = chunks.value.every(c => c.status === 'success');
        if (!allSuccess) {
            ElMessage.error('存在未上传成功的分片，无法完成');
            return false;
        }

        try {
            console.log('=== 开始完成上传流程 ===');

            // 获取当前用户ID
            const currentUserId = getCurrentUserId();
            if (!currentUserId) {
                ElMessage.error('无法获取当前用户ID');
                return false;
            }
            console.log('当前用户ID:', currentUserId);

            // 使用保存的 AES 密钥对象
            const aesKeyObjValue = aesKeyObj.value;
            console.log('AES 密钥对象类型:', aesKeyObjValue?.constructor.name);

            // 1. 获取所有接收者的公钥（包括上传者自己和其他接收者）
            const recipientPublicKeyMap = new Map(); // 用于去重

            // 1.1 先添加上传者自己
            try {
                console.log('获取上传者自己的公钥, userId:', currentUserId);
                const selfRes = await request.get(`/api/v1/key/user/${currentUserId}`);
                console.log('获取自己公钥响应:', selfRes.data);

                // 正确的公钥在 data.publicKey 字段中
                const userKeyInfo = selfRes.data.data;
                if (!userKeyInfo) {
                    throw new Error('响应中没有用户密钥信息');
                }

                const selfPublicKey = userKeyInfo.publicKey;
                console.log('公钥类型:', typeof selfPublicKey);
                console.log('公钥前100字符:', selfPublicKey?.substring(0, 100));

                if (!selfPublicKey) {
                    throw new Error('自己的公钥为空');
                }

                recipientPublicKeyMap.set(currentUserId, {
                    userId: currentUserId,
                    publicKey: selfPublicKey
                });
                console.log('获取自己公钥成功');
            } catch (err) {
                console.error('获取当前用户公钥失败:', err);
                ElMessage.error('请先在密钥管理页面上传公钥');
                return false;
            }

            // 1.2 添加其他接收者（如果有）
            if (recipients.value.length > 0) {
                for (const r of recipients.value) {
                    // 如果已经添加过（比如是自己），则跳过
                    if (recipientPublicKeyMap.has(r.userId)) {
                        console.log('接收者已存在，跳过:', r.userId);
                        continue;
                    }

                    try {
                        console.log(`获取接收者 ${r.userId} 的公钥`);
                        const res = await request.get(`/api/v1/key/user/${r.userId}`);

                        const userKeyInfo = res.data.data;
                        if (!userKeyInfo) {
                            throw new Error('响应中没有用户密钥信息');
                        }

                        const publicKey = userKeyInfo.publicKey;
                        if (!publicKey) {
                            throw new Error('公钥为空');
                        }

                        recipientPublicKeyMap.set(r.userId, {
                            userId: r.userId,
                            publicKey: publicKey
                        });
                        console.log('获取接收者公钥成功:', r.userId);
                    } catch (err) {
                        console.error(`获取用户 ${r.userId} 公钥失败:`, err);
                        ElMessage.error(`用户 ${r.userId} 未上传公钥`);
                        return false;
                    }
                }
            }

            // 转换为数组
            const recipientPublicKeyList = Array.from(recipientPublicKeyMap.values());
            console.log('最终接收者列表:', recipientPublicKeyList.map(item => item.userId));

            // 检查是否有接收者
            if (recipientPublicKeyList.length === 0) {
                ElMessage.error('没有有效的接收者');
                return false;
            }

            // 2. 加密AES密钥给所有接收者
            const recipientsWrappedKeys = [];

            for (const item of recipientPublicKeyList) {
                try {
                    console.log(`开始为用户 ${item.userId} 加密AES密钥`);

                    // 确保公钥是字符串
                    let publicKeyStr = item.publicKey;
                    if (typeof publicKeyStr !== 'string') {
                        publicKeyStr = String(publicKeyStr);
                    }
                    publicKeyStr = publicKeyStr.trim();

                    // 导入公钥
                    const publicKey = await importPublicKey(publicKeyStr);

                    // 加密AES密钥 - 使用保存的 AES 密钥对象
                    const wrappedKey = await wrapAESKeyWithRSA(aesKeyObjValue, publicKey);

                    recipientsWrappedKeys.push({
                        recipientUserId: Number(item.userId), // 确保是数字类型
                        wrappedAesKey: wrappedKey
                    });

                    console.log(`为用户 ${item.userId} 加密AES密钥成功, 长度:`, wrappedKey.length);
                } catch (err) {
                    console.error(`为用户 ${item.userId} 加密AES密钥失败:`, err);
                    console.error('公钥内容:', item.publicKey);
                    ElMessage.error(`为用户 ${item.userId} 加密失败`);
                    return false;
                }
            }

            // 3. 计算文件明文 SHA256
            const plainSha256 = await calculateSHA256(file.value);

            // 4. 调用完成上传接口
            console.log('发送完成上传请求，接收者数量:', recipientsWrappedKeys.length);
            console.log('请求数据:', {
                fileId: fileId.value,
                plainSha256: plainSha256,
                recipientsWrappedKeys: recipientsWrappedKeys.map(item => ({
                    recipientUserId: item.recipientUserId,
                    wrappedAesKey: item.wrappedAesKey.substring(0, 50) + '...'
                }))
            });

            const res = await request.post('/api/v1/file/upload/complete', {
                fileId: fileId.value,
                plainSha256: plainSha256,
                recipientsWrappedKeys: recipientsWrappedKeys
            });

            console.log('完成上传响应:', res.data);
            ElMessage.success('文件上传完成！');
            return true;
        } catch (err) {
            console.error('完成上传失败详情：', err);
            ElMessage.error('完成上传失败');
            return false;
        }
    };

    // 暂停上传
    const pauseUpload = (): void => {
        isPaused.value = true;
        isUploading.value = false;
        ElMessage.info('已暂停上传');
    };

    // 继续上传
    const resumeUpload = (): void => {
        // 重置失败分片状态，允许重试
        chunks.value
            .filter(c => c.status === 'failed')
            .forEach(c => {
                c.status = 'waiting';
                c.retryCount = 0;
            });

        uploadChunks();
    };

    // 取消上传（清空所有状态）
    const cancelUpload = (): void => {
        file.value = null;
        fileId.value = '';
        chunks.value = [];
        aesKey.value = null;
        aesKeyObj.value = null;
        isUploading.value = false;
        isPaused.value = false;
        ElMessage.info('已取消上传');
    };

    // 选择文件（直接赋值，不清空原有状态）
    const selectFile = (selectedFile: File): void => {
        console.log('选择文件:', selectedFile.name, selectedFile.size);
        // 重置状态但不显示取消消息
        file.value = selectedFile;
        fileId.value = '';
        chunks.value = [];
        aesKey.value = null;
        aesKeyObj.value = null;
        isUploading.value = false;
        isPaused.value = false;
    };

    return {
        // 状态（供页面组件使用）
        file,
        fileId,
        extractCode,
        expiryHours,
        downloadLimit,
        burnAfterReading,
        recipients,
        chunks,
        progress,
        isUploading,
        isPaused,

        // 方法（供页面组件调用）
        selectFile,
        initUpload,
        uploadChunks,
        completeUpload,
        pauseUpload,
        resumeUpload,
        cancelUpload
    };
}