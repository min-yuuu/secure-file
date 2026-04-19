package com.securefile.storage;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * 本地文件存储
 */
@Slf4j
@Component
public class LocalFileStorage {

    @Value("${file.storage.base-path:D:/files}")
    private String basePath;

    /**
     * 保存分片
     * 
     * @param fileId 文件ID
     * @param idx    分片索引
     * @param bytes  分片数据
     * @return 存储路径
     */
    public String saveChunk(String fileId, int idx, byte[] bytes) {
        String path = buildChunkPath(fileId, idx);
        File file = new File(path);

        try {
            FileUtil.mkParentDirs(file);
            FileUtil.writeBytes(bytes, file);
            log.info("分片保存成功: fileId={}, idx={}, path={}", fileId, idx, path);
            return path;
        } catch (Exception e) {
            log.error("分片保存失败: fileId={}, idx={}", fileId, idx, e);
            throw new RuntimeException("分片保存失败", e);
        }
    }

    /**
     * 删除文件目录
     * 
     * @param fileId 文件ID
     */
    public void deleteFileDir(String fileId) {
        String dirPath = basePath + File.separator + fileId;
        File dir = new File(dirPath);

        if (dir.exists()) {
            FileUtil.del(dir);
            log.info("文件目录删除成功: fileId={}, path={}", fileId, dirPath);
        }
    }

    /**
     * 构建分片路径
     */
    private String buildChunkPath(String fileId, int idx) {
        return basePath + File.separator + fileId +
                File.separator + "chunks" +
                File.separator + idx + ".bin";
    }
}
