package com.securefile.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * 下载申请响应
 */
@ApiModel("下载申请响应")
@Data
public class DownloadApplyVO {
    @ApiModelProperty("下载会话ID")
    private String downloadSessionId;

    @ApiModelProperty("封装的AES密钥（用接收者公钥加密）")
    private String wrappedAesKey;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件大小")
    private Long fileSize;

    @ApiModelProperty("分片总数")
    private Integer chunkCount;

    @ApiModelProperty("分片元信息列表")
    private List<ChunkMeta> chunkMetaList;

    @ApiModel("分片元信息")
    @Data
    public static class ChunkMeta {
        @ApiModelProperty("分片索引")
        private Integer chunkIndex;

        @ApiModelProperty("分片大小")
        private Long chunkSize;

        @ApiModelProperty("密文SHA256")
        private String cipherSha256;
    }
}
