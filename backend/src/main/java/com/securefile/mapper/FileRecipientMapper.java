package com.securefile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.securefile.entity.Recipient;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface FileRecipientMapper extends BaseMapper<Recipient> {

    /**
     * 批量插入接收者
     */
    void batchInsert(@Param("list") List<Recipient> recipients);
}