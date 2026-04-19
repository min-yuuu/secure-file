/**
 * 接收者信息类型
 */
export interface Recipient {
    userId: number;
    username: string;
    publicKey?: string; // 可选，接收者公钥
}

/**
 * 初始化上传请求参数（严格匹配后端接口）
 */
export interface UploadInitRequest {
    fileName: string;
    fileSize: number;
    chunkCount: number;
    extractCode?: string;
    expiryHours: number;
    downloadLimit: number;
    burnAfterReading: boolean;
    contentType: string;
    plainSha256: string;
}

/**
 * 初始化上传响应类型
 */
export interface UploadInitResponse {
    code: number;
    msg: string;
    data: {
        fileId: string;
        uploadedChunks: number[]; // 已上传的分片索引
    };
}

/**
 * 单个分片信息
 */
export interface ChunkInfo {
    index: number;
    size: number;
    data: Blob;
    status: 'waiting' | 'uploading' | 'success' | 'failed';
    retryCount: number;
}

/**
 * 完成上传请求参数
 */
export interface UploadCompleteRequest {
    fileId: string;
    plainSha256: string;
    recipientsWrappedKeys: Array<{
        recipientUserId: number;
        wrappedAesKey: string;
    }>;
}

/**
 * 已上传文件列表项
 */
export interface UploadedFileItem {
    fileId: string;
    fileName: string;
    fileSize: number;
    status: 'UPLOADING' | 'COMPLETE' | 'DELETED';
    createdAt: string;
    downloadCount: number;
    shareCode?: string;
    extractCode?: string;
    expiryTime?: string;
}