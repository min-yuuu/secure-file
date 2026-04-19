package com.securefile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.securefile.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM user WHERE username = #{username} LIMIT 1")
    User selectByUsername(@Param("username") String username);
    
    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') LIMIT #{limit}")
    java.util.List<User> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}
