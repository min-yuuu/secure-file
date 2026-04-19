package com.securefile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.securefile.entity.Chunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 文件分片Mapper
 */
@Mapper
public interface FileChunkMapper extends BaseMapper<Chunk> {

    @Select("SELECT chunk_index FROM chunk WHERE file_id = #{fileId} ORDER BY chunk_index")
    List<Integer> selectUploadedIndexes(@Param("fileId") String fileId);

    @Select("SELECT COUNT(*) FROM chunk WHERE file_id = #{fileId}")
    Integer countByFileId(@Param("fileId") String fileId);
}
