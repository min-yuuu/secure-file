package com.securefile.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * 上传初始化响应
 */
@ApiModel("上传初始化响应")
@Data
public class UploadInitVO {
    @ApiModelProperty("文件ID")
    private String fileId;

    @ApiModelProperty("已上传的分片索引列表（用于断点续传）")
    private List<Integer> uploadedChunkIndexList;
}
