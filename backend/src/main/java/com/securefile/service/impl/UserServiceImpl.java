package com.securefile.service.impl;
import cn.hutool.crypto.digest.BCrypt;
import com.securefile.common.JwtUtils;
import com.securefile.entity.User;
import com.securefile.exception.BizException;
import com.securefile.mapper.UserMapper;
import com.securefile.service.UserService;
import com.securefile.vo.PublicKeyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.securefile.common.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 
 * @author:zoujingmin
 * @date: 2026/1/26
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public String register(String username, String password) {
        // 1. 检查重名
        if (userMapper.selectByUsername(username) != null) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS);
        }
        // 2. 密码加盐哈希存储 (底座安全要求)
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return "注册成功";
    }
    
    @Override
    public String login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        // 1. 校验密码
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException(ErrorCode.INVALID_PASSWORD);
        }
        // 2. 生成 Token 并返回 (底座鉴权任务)
        return JwtUtils.createToken(user.getId(), user.getUsername());
    }
    
    @Override
    public boolean saveUserPublicKey(Long userId, String publicKey) {
        User user = new User();
        user.setId(userId);
        user.setPublicKey(publicKey);
        // 只更新公钥字段
        return userMapper.updateById(user) > 0;
    }
    
    @Override
    public String getPublicKey(Long userId) {
        User user = userMapper.selectById(userId);
        return (user != null) ? user.getPublicKey() : null;
    }
    
    @Override
    public PublicKeyVO getUserPublicKey(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        
        PublicKeyVO vo = new PublicKeyVO();
        vo.setUserId(String.valueOf(user.getId()));
        vo.setUsername(user.getUsername());
        vo.setPublicKey(user.getPublicKey());
        return vo;
    }
    
    @Override
    public List<PublicKeyVO> getUserPublicKeys(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return userIds.stream()
                .map(userId -> {
                    try {
                        return getUserPublicKey(userId);
                    } catch (BizException e) {
                        // 如果用户不存在，返回null，后续过滤掉
                        return null;
                    }
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());
    }
    
    @Override
    public User getById(Long userId) {
        return userMapper.selectById(userId);
    }
    
    @Override
    public List<PublicKeyVO> searchPublicKeys(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        if (limit <= 0) limit = 10;
        List<User> users = userMapper.searchByKeyword(keyword.trim(), limit);
        return users.stream()
                .filter(u -> u.getPublicKey() != null && !u.getPublicKey().isEmpty())
                .map(u -> {
                    PublicKeyVO vo = new PublicKeyVO();
                    vo.setUserId(String.valueOf(u.getId()));
                    vo.setUsername(u.getUsername());
                    vo.setPublicKey(u.getPublicKey());
                    vo.setCreateTime(u.getCreateTime());
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
