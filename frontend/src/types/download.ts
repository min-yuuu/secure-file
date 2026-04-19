/**
 * 下载相关类型定义
 */

/**
 * 下载申请请求DTO
 */
export interface DownloadApplyDTO {
    fileId: string;
    extractCode?: string;
}

/**
 * 分片元信息
 */
export interface ChunkMeta {
    chunkIndex: number;
    chunkSize: number;
    cipherSha256: string;
}

/**
 * 下载申请响应VO
 */
export interface DownloadApplyVO {
    downloadSessionId: string;
    wrappedAesKey: string;
    fileName: string;
    fileSize: number;
    chunkCount: number;
    chunkMetaList: ChunkMeta[];
}

/**
 * 下载任务状态
 */
export type DownloadStatus = 'pending' | 'applying' | 'decrypting' | 'downloading' | 'paused' | 'completed' | 'failed' | 'cancelled';

/**
 * 下载任务
 */
export interface DownloadTask {
    id: string;                     // 任务ID
    fileId: string;                 // 文件ID
    fileName: string;               // 文件名
    fileSize: number;               // 文件大小
    status: DownloadStatus;         // 状态
    progress: number;               // 进度 0-100
    sessionId?: string;             // 下载会话ID
    wrappedAesKey?: string;         // 封装的AES密钥
    chunkMetas?: ChunkMeta[];       // 分片元信息
    chunks: ChunkTask[];            // 分片任务列表
    error?: string;                 // 错误信息
    createdAt: number;              // 创建时间戳
    completedChunks: number;        // 已完成分片数
    totalChunks: number;            // 总分片数
    downloadedChunks: ArrayBuffer[]; // 已下载的分片数据
    blobUrl?: string;               // 生成的Blob URL（完成后）
}

/**
 * 分片下载任务
 */
export interface ChunkTask {
    index: number;
    size: number;
    sha256: string;
    status: 'pending' | 'downloading' | 'completed' | 'failed';
    retryCount: number;
    data?: ArrayBuffer;             // 分片数据
}

/**
 * 文件信息
 */
export interface FileInfo {
    fileId: string;
    fileName: string;
    fileSize: number;
    contentType?: string;
    userId: number;
    username?: string;
    status: string;
    createdAt: string;
    updatedAt: string;
    shareCode?: string;
    extractCode?: string;
    expiryTime?: string;
    downloadLimit?: number;
    downloadCount?: number;
    burnAfterReading?: boolean;
    plainSha256?: string;
}

/**
 * 文件详情（包含接收者列表）
 */
export interface FileDetail extends FileInfo {
    recipients?: Array<{
        userId: number;
        username?: string;
        wrappedAesKey: string;
        createdAt?: string;
    }>;
    downloadRecords?: DownloadRecord[];
}

/**
 * 下载记录
 */
export interface DownloadRecord {
    id: number;
    fileId: string;
    userId: number;
    username?: string;
    ip?: string;
    createTime: string;
}

/**
 * 审计日志条目
 */
export interface AuditLog {
    id: number;
    fileId: string;
    fileName?: string;
    userId: number;
    username?: string;
    action: 'UPLOAD' | 'DOWNLOAD';
    ip?: string;
    userAgent?: string;
    createTime: string;
}

/**
 * 分页响应
 */
export interface PageResult<T> {
    list: T[];
    total: number;
    current: number;
    size: number;
    pages: number;
}

/**
 * 趋势数据点
 */
export interface TrendDataPoint {
    date: string;
    upload: number;
    download: number;
}

/**
 * 下载队列
 */
export interface DownloadQueue {
    tasks: DownloadTask[];
    activeCount: number;
    maxConcurrent: number;
}