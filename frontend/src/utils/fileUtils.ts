/**
 * 文件处理工具函数
 * 职责：文件下载、Blob处理、完整性校验等
 */

import { ElMessage } from 'element-plus';

/**
 * 将ArrayBuffer合并并保存为文件
 * @param buffers ArrayBuffer数组
 * @param fileName 文件名
 * @param contentType MIME类型
 */
export const saveAsFile = (
    buffers: ArrayBuffer[],
    fileName: string,
    contentType: string = 'application/octet-stream'
): void => {
    try {
        // 计算总大小
        const totalSize = buffers.reduce((acc, buf) => acc + buf.byteLength, 0);

        // 合并ArrayBuffer
        const mergedBuffer = new Uint8Array(totalSize);
        let offset = 0;

        for (const buffer of buffers) {
            mergedBuffer.set(new Uint8Array(buffer), offset);
            offset += buffer.byteLength;
        }

        // 创建Blob
        const blob = new Blob([mergedBuffer], { type: contentType });

        // 创建下载链接
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        // 清理
        window.URL.revokeObjectURL(url);

        console.log(`文件保存成功: ${fileName}, 大小: ${formatFileSize(totalSize)}`);
    } catch (error) {
        console.error('保存文件失败:', error);
        ElMessage.error('文件保存失败');
        throw error;
    }
};

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @returns 格式化后的字符串
 */
export const formatFileSize = (bytes?: number): string => {
    if (bytes === undefined || bytes === null) return '-';
    if (bytes === 0) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
};

/**
 * 格式化日期时间
 * @param dateStr 日期字符串
 * @returns 格式化后的日期时间
 */
export const formatDateTime = (dateStr?: string): string => {
    if (!dateStr) return '-';
    try {
        const date = new Date(dateStr);
        return date.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    } catch {
        return dateStr;
    }
};

/**
 * 格式化日期（不含时间）
 * @param dateStr 日期字符串
 * @returns 格式化后的日期
 */
export const formatDate = (dateStr?: string): string => {
    if (!dateStr) return '-';
    try {
        const date = new Date(dateStr);
        return date.toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
    } catch {
        return dateStr;
    }
};

/**
 * 计算进度百分比
 * @param completed 已完成数
 * @param total 总数
 * @returns 百分比（0-100）
 */
export const calculateProgress = (completed: number, total: number): number => {
    if (total === 0) return 0;
    return Math.min(100, Math.round((completed / total) * 100));
};

/**
 * 验证分片完整性
 * @param data 分片数据
 * @param expectedSha256 期望的SHA256
 * @returns 是否完整
 */
export const verifyChunkIntegrity = async (
    data: ArrayBuffer,
    expectedSha256: string
): Promise<boolean> => {
    try {
        // 使用crypto.js中的sha256Hex函数
        const { sha256Hex } = await import('./crypto');
        const actualSha256 = await sha256Hex(data);
        const isValid = actualSha256 === expectedSha256;

        if (!isValid) {
            console.warn('分片完整性校验失败', { expectedSha256, actualSha256 });
        }

        return isValid;
    } catch (error) {
        console.error('校验分片失败:', error);
        return false;
    }
};

/**
 * 安全的文件名（移除路径和非法字符）
 * @param fileName 原始文件名
 * @returns 安全文件名
 */
export const safeFileName = (fileName: string): string => {
    // 移除路径分隔符
    let safe = fileName.replace(/[/\\:*?"<>|]/g, '_');
    // 限制长度
    if (safe.length > 255) {
        const ext = getFileExtension(safe);
        const name = safe.slice(0, 255 - ext.length);
        safe = name + ext;
    }
    return safe;
};

/**
 * 从文件名获取文件扩展名
 * @param fileName 文件名
 * @returns 扩展名（包含点，如.pdf）
 */
export const getFileExtension = (fileName: string): string => {
    const lastDotIndex = fileName.lastIndexOf('.');
    return lastDotIndex === -1 ? '' : fileName.slice(lastDotIndex);
};

/**
 * IndexedDB 存储管理器
 * 用于持久化已下载的分片，支持断点续传
 */
export class ChunkStorage {
    private dbName: string;
    private storeName: string;
    private db: IDBDatabase | null = null;

    constructor(dbName: string = 'FileDownloadDB', storeName: string = 'chunks') {
        this.dbName = dbName;
        this.storeName = storeName;
    }

    /**
     * 打开数据库
     */
    async open(): Promise<void> {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.dbName, 1);

            request.onerror = () => reject(request.error);
            request.onsuccess = () => {
                this.db = request.result;
                resolve();
            };

            request.onupgradeneeded = (event) => {
                const db = (event.target as IDBOpenDBRequest).result;
                if (!db.objectStoreNames.contains(this.storeName)) {
                    // 创建存储对象，使用 fileId+chunkIndex 作为键
                    const store = db.createObjectStore(this.storeName, { keyPath: 'id' });
                    store.createIndex('fileId', 'fileId', { unique: false });
                    store.createIndex('timestamp', 'timestamp', { unique: false });
                }
            };
        });
    }

    /**
     * 保存分片
     * @param fileId 文件ID
     * @param chunkIndex 分片索引
     * @param data 分片数据
     * @param sha256 分片SHA256
     */
    async saveChunk(
        fileId: string,
        chunkIndex: number,
        data: ArrayBuffer,
        sha256: string
    ): Promise<void> {
        if (!this.db) await this.open();

        return new Promise((resolve, reject) => {
            const transaction = this.db!.transaction([this.storeName], 'readwrite');
            const store = transaction.objectStore(this.storeName);

            const request = store.put({
                id: `${fileId}_${chunkIndex}`,
                fileId,
                chunkIndex,
                data,
                sha256,
                timestamp: Date.now()
            });

            request.onerror = () => reject(request.error);
            request.onsuccess = () => resolve();
        });
    }

    /**
     * 获取分片
     * @param fileId 文件ID
     * @param chunkIndex 分片索引
     */
    async getChunk(fileId: string, chunkIndex: number): Promise<ArrayBuffer | null> {
        if (!this.db) await this.open();

        return new Promise((resolve, reject) => {
            const transaction = this.db!.transaction([this.storeName], 'readonly');
            const store = transaction.objectStore(this.storeName);
            const request = store.get(`${fileId}_${chunkIndex}`);

            request.onerror = () => reject(request.error);
            request.onsuccess = () => {
                const result = request.result;
                resolve(result ? result.data : null);
            };
        });
    }

    /**
     * 获取文件的所有已保存分片
     * @param fileId 文件ID
     */
    async getFileChunks(fileId: string): Promise<Map<number, ArrayBuffer>> {
        if (!this.db) await this.open();

        return new Promise((resolve, reject) => {
            const transaction = this.db!.transaction([this.storeName], 'readonly');
            const store = transaction.objectStore(this.storeName);
            const index = store.index('fileId');
            const request = index.getAll(fileId);

            request.onerror = () => reject(request.error);
            request.onsuccess = () => {
                const chunks = request.result || [];
                const map = new Map<number, ArrayBuffer>();
                chunks.forEach((chunk: any) => {
                    map.set(chunk.chunkIndex, chunk.data);
                });
                resolve(map);
            };
        });
    }

    /**
     * 删除文件的所有分片
     * @param fileId 文件ID
     */
    async deleteFileChunks(fileId: string): Promise<void> {
        if (!this.db) await this.open();

        return new Promise((resolve, reject) => {
            const transaction = this.db!.transaction([this.storeName], 'readwrite');
            const store = transaction.objectStore(this.storeName);
            const index = store.index('fileId');
            const request = index.openCursor(fileId);

            request.onerror = () => reject(request.error);
            request.onsuccess = (event) => {
                const cursor = (event.target as IDBRequest).result;
                if (cursor) {
                    cursor.delete();
                    cursor.continue();
                } else {
                    resolve();
                }
            };
        });
    }

    /**
     * 清理过期分片（超过24小时）
     */
    async cleanExpired(): Promise<void> {
        if (!this.db) await this.open();

        const oneDayAgo = Date.now() - 24 * 60 * 60 * 1000;

        return new Promise((resolve, reject) => {
            const transaction = this.db!.transaction([this.storeName], 'readwrite');
            const store = transaction.objectStore(this.storeName);
            const index = store.index('timestamp');
            const request = index.openCursor(IDBKeyRange.upperBound(oneDayAgo));

            request.onerror = () => reject(request.error);
            request.onsuccess = (event) => {
                const cursor = (event.target as IDBRequest).result;
                if (cursor) {
                    cursor.delete();
                    cursor.continue();
                } else {
                    resolve();
                }
            };
        });
    }
}

// 导出单例
export const chunkStorage = new ChunkStorage();