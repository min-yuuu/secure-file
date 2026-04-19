package com.securefile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.securefile.entity.Session;
import org.apache.ibatis.annotations.Mapper;

/**
 * 下载会话Mapper
 */
@Mapper
public interface DownloadSessionMapper extends BaseMapper<Session> {
}
