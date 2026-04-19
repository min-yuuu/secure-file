package com.securefile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.securefile.entity.File;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件Mapper
 */
@Mapper
public interface FileMapper extends BaseMapper<File> {
}
