<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.damien.tradewise.user.mapper.TwPasswordResetTokenMapper">
    
    <insert id="insert" parameterType="com.damien.tradewise.user.entity.TwPasswordResetToken" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO tw_password_reset_tokens (user_id, token, email, expires_at, used)
        VALUES (#{userId}, #{token}, #{email}, #{expiresAt}, #{used})
    </insert>
    
    <select id="findByToken" resultType="com.damien.tradewise.user.entity.TwPasswordResetToken">
        SELECT * FROM tw_password_reset_tokens
        WHERE token = #{token} AND used = 0 AND expires_at > NOW()
    </select>
    
    <update id="markAsUsed">
        UPDATE tw_password_reset_tokens
        SET used = 1
        WHERE token = #{token}
    </update>
    
    <delete id="deleteExpiredTokens">
        DELETE FROM tw_password_reset_tokens
        WHERE expires_at &lt; NOW() OR used = 1
    </delete>
    
</mapper>
