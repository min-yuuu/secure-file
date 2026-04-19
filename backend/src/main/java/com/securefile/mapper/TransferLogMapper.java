package com.securefile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.securefile.entity.Log;
import org.apache.ibatis.annotations.Mapper;

/**
 * 传输日志Mapper
 */
@Mapper
public interface TransferLogMapper extends BaseMapper<Log> {
}
