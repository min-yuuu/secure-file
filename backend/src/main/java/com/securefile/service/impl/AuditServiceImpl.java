package com.securefile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.securefile.common.PageResult;
import com.securefile.entity.Log;
import com.securefile.entity.User;
import com.securefile.mapper.TransferLogMapper;
import com.securefile.mapper.UserMapper;
import com.securefile.service.AuditService;
import com.securefile.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final TransferLogMapper transferLogMapper;
    private final UserMapper userMapper;

    @Override
    public void logUpload(String fileId, Long userId, String ip, String userAgent) {
        Log logEntity = new Log();
        logEntity.setFileId(fileId);
        logEntity.setUserId(userId);
        logEntity.setAction("UPLOAD");
        logEntity.setIp(ip);
        logEntity.setUserAgent(userAgent);
        logEntity.setCreateTime(LocalDateTime.now());
        transferLogMapper.insert(logEntity);
        log.info("记录上传日志, fileId={}, userId={}, ip={}", fileId, userId, ip);
    }

    @Override
    public void logDownload(String fileId, Long userId, String ip, String userAgent) {
        Log logEntity = new Log();
        logEntity.setFileId(fileId);
        logEntity.setUserId(userId);
        logEntity.setAction("DOWNLOAD");
        logEntity.setIp(ip);
        logEntity.setUserAgent(userAgent);
        logEntity.setCreateTime(LocalDateTime.now());
        transferLogMapper.insert(logEntity);
        log.info("记录下载日志, fileId={}, userId={}, ip={}", fileId, userId, ip);
    }

    @Override
    public PageResult<AuditLogVO> pageTransferLogs(Integer current, Integer size, String action, Long userId) {
        Page<Log> page = new Page<>(current, size);

        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isEmpty()) {
            wrapper.eq(Log::getAction, action);
        }
        if (userId != null) {
            wrapper.eq(Log::getUserId, userId);
        }
        wrapper.orderByDesc(Log::getCreateTime);

        IPage<Log> result = transferLogMapper.selectPage(page, wrapper);

        // 获取所有用户ID
        List<Long> userIds = result.getRecords().stream()
                .map(Log::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询用户名
        Map<Long, String> usernameMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            // 修复 lambda 表达式错误
            for (User user : users) {
                if (user != null) {
                    usernameMap.put(user.getId(), user.getUsername());
                }
            }
        }

        // 转换为VO - 使用传统for循环避免lambda变量问题
        List<AuditLogVO> voList = new java.util.ArrayList<>();
        for (Log logEntity : result.getRecords()) {
            AuditLogVO vo = new AuditLogVO();
            vo.setId(logEntity.getId());
            vo.setFileId(logEntity.getFileId());
            // 从文件表获取文件名，如果没有则使用默认
            vo.setFileName("文件_" + (logEntity.getFileId().length() > 8 ?
                    logEntity.getFileId().substring(0, 8) : logEntity.getFileId()));
            vo.setUserId(logEntity.getUserId());
            vo.setUsername(usernameMap.getOrDefault(logEntity.getUserId(), "未知用户"));
            vo.setAction(logEntity.getAction());
            vo.setIp(logEntity.getIp());
            vo.setUserAgent(logEntity.getUserAgent());
            vo.setCreateTime(logEntity.getCreateTime());
            voList.add(vo);
        }

        return PageResult.of(
                voList,
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }
}