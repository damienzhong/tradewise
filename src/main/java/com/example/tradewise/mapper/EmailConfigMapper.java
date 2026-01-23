package com.example.tradewise.mapper;

import com.example.tradewise.entity.EmailConfig;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface EmailConfigMapper {

    @Insert("INSERT INTO email_config (email_address, enabled) VALUES (#{emailAddress}, #{enabled})")
    void insert(EmailConfig emailConfig);

    @Update("UPDATE email_config SET email_address = #{emailAddress}, enabled = #{enabled} WHERE id = #{id}")
    void update(EmailConfig emailConfig);

    @Select("SELECT * FROM email_config WHERE enabled = 1")
    List<EmailConfig> findAllEnabled();

    @Select("SELECT * FROM email_config WHERE email_address = #{emailAddress}")
    EmailConfig findByEmailAddress(@Param("emailAddress") String emailAddress);

    @Delete("DELETE FROM email_config WHERE id = #{id}")
    void deleteById(@Param("id") Integer id);

    @Delete("DELETE FROM email_config WHERE email_address = #{emailAddress}")
    void deleteByEmailAddress(@Param("emailAddress") String emailAddress);

    @Select("SELECT COUNT(*) FROM email_config")
    int countAll();
}