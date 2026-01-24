package com.example.tradewise.mapper;

import com.example.tradewise.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM users WHERE enabled = 1")
    List<User> findAllEnabled();

    @Insert("INSERT INTO users (username, password, role, enabled, created_at) " +
            "VALUES (#{username}, #{password}, #{role}, #{enabled}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE users SET password = #{password}, role = #{role}, enabled = #{enabled} WHERE id = #{id}")
    int update(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Long id);
}