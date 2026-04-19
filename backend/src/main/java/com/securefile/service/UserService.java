package com.securefile.service;

import com.securefile.entity.User;
import com.securefile.vo.PublicKeyVO;

import java.util.List;

public interface UserService {
    // 账号逻辑
    String register(String username, String password);
    String login(String username, String password);

    // 密钥管理
    boolean saveUserPublicKey(Long userId, String publicKey);
    String getPublicKey(Long userId);
    PublicKeyVO getUserPublicKey(Long userId);
    List<PublicKeyVO> getUserPublicKeys(List<Long> userIds);

    // 用户搜索
    List<PublicKeyVO> searchPublicKeys(String keyword, int limit);

    // 通用查询
    User getById(Long userId);
}
