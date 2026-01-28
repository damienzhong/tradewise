<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.damien.tradewise.admin.mapper.TwSystemEmailConfigMapper">
    
    <select id="findAll" resultType="com.damien.tradewise.admin.entity.TwSystemEmailConfig">
        SELECT * FROM tw_system_email_config ORDER BY is_default DESC, created_at DESC
    </select>
    
    <select id="findById" resultType="com.damien.tradewise.admin.entity.TwSystemEmailConfig">
        SELECT * FROM tw_system_email_config WHERE id = #{id}
    </select>
    
    <select id="findDefault" resultType="com.damien.tradewise.admin.entity.TwSystemEmailConfig">
        SELECT * FROM tw_system_email_config WHERE is_default = 1 AND enabled = 1 LIMIT 1
    </select>
    
    <insert id="insert" parameterType="com.damien.tradewise.admin.entity.TwSystemEmailConfig" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO tw_system_email_config (email_name, smtp_host, smtp_port, username, password, from_address, from_name, use_ssl, use_tls, is_default, enabled, test_status)
        VALUES (#{emailName}, #{smtpHost}, #{smtpPort}, #{username}, #{password}, #{fromAddress}, #{fromName}, #{useSsl}, #{useTls}, #{isDefault}, #{enabled}, #{testStatus})
    </insert>
    
    <update id="update" parameterType="com.damien.tradewise.admin.entity.TwSystemEmailConfig">
        UPDATE tw_system_email_config
        SET email_name = #{emailName},
            smtp_host = #{smtpHost},
            smtp_port = #{smtpPort},
            username = #{username},
            password = #{password},
            from_address = #{fromAddress},
            from_name = #{fromName},
            use_ssl = #{useSsl},
            use_tls = #{useTls},
            is_default = #{isDefault},
            enabled = #{enabled}
        WHERE id = #{id}
    </update>
    
    <delete id="delete">
        DELETE FROM tw_system_email_config WHERE id = #{id}
    </delete>
    
    <update id="clearDefault">
        UPDATE tw_system_email_config SET is_default = 0
    </update>
    
    <update id="updateTestStatus">
        UPDATE tw_system_email_config
        SET test_status = #{status}, last_test_time = NOW()
        WHERE id = #{id}
    </update>
    
</mapper>
