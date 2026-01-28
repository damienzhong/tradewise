<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.damien.tradewise.admin.mapper.TwAdminPasswordResetTokenMapper">
    
    <insert id="insert" parameterType="com.damien.tradewise.admin.entity.TwAdminPasswordResetToken" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO tw_admin_password_reset_tokens (admin_id, token, email, expires_at, used)
        VALUES (#{adminId}, #{token}, #{email}, #{expiresAt}, #{used})
    </insert>
    
    <select id="findByToken" resultType="com.damien.tradewise.admin.entity.TwAdminPasswordResetToken">
        SELECT * FROM tw_admin_password_reset_tokens
        WHERE token = #{token} AND used = 0 AND expires_at > NOW()
    </select>
    
    <update id="markAsUsed">
        UPDATE tw_admin_password_reset_tokens
        SET used = 1
        WHERE token = #{token}
    </update>
    
</mapper>
