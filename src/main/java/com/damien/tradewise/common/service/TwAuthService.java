package com.damien.tradewise.common.service;

import com.damien.tradewise.admin.entity.TwAdmin;
import com.damien.tradewise.admin.entity.TwAdminPasswordResetToken;
import com.damien.tradewise.user.entity.TwEmailVerificationToken;
import com.damien.tradewise.user.entity.TwPasswordResetToken;
import com.damien.tradewise.user.entity.TwUser;
import com.damien.tradewise.admin.mapper.TwAdminMapper;
import com.damien.tradewise.admin.mapper.TwAdminPasswordResetTokenMapper;
import com.damien.tradewise.user.mapper.TwEmailVerificationTokenMapper;
import com.damien.tradewise.user.mapper.TwPasswordResetTokenMapper;
import com.damien.tradewise.user.mapper.TwUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证服务
 */
@Service
public class TwAuthService {
    
    @Autowired
    private TwAdminMapper adminMapper;
    
    @Autowired
    private TwUserMapper userMapper;
    
    @Autowired
    private TwPasswordEncoder passwordEncoder;
    
    @Autowired
    private TwPasswordResetTokenMapper resetTokenMapper;
    
    @Autowired
    private TwAdminPasswordResetTokenMapper adminResetTokenMapper;
    
    @Autowired
    private TwEmailVerificationTokenMapper emailVerificationTokenMapper;
    
    @Autowired
    private TwEmailService emailService;
    
    @Value("${tradewise.app.domain}")
    private String appDomain;
    
    /**
     * 管理员登录认证
     */
    public TwAdmin authenticateAdmin(String username, String password) {
        TwAdmin admin = adminMapper.findByUsername(username);
        if (admin == null || !admin.getEnabled()) {
            return null;
        }
        
        if (passwordEncoder.matches(password, admin.getPassword())) {
            return admin;
        }
        
        return null;
    }
    
    /**
     * 用户登录认证
     */
    public TwUser authenticateUser(String username, String password) {
        TwUser user = userMapper.findByUsername(username);
        if (user == null || !user.getEnabled()) {
            return null;
        }
        
        if (passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        
        return null;
    }
    
    /**
     * 用户注册
     */
    public TwUser registerUser(String username, String password, String email) {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        TwUser user = new TwUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setEmailVerified(false);
        user.setEnabled(true);
        
        userMapper.insert(user);
        
        // 发送邮箱验证邮件
        if (email != null && !email.isEmpty()) {
            sendEmailVerification(user);
        }
        
        return user;
    }
    
    /**
     * 发送邮箱验证邮件
     */
    public void sendEmailVerification(TwUser user) {
        String token = UUID.randomUUID().toString();
        
        TwEmailVerificationToken verificationToken = new TwEmailVerificationToken();
        verificationToken.setUserId(user.getId());
        verificationToken.setToken(token);
        verificationToken.setEmail(user.getEmail());
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24小时后过期
        verificationToken.setUsed(false);
        
        emailVerificationTokenMapper.insert(verificationToken);
        
        // 发送邮件
        try {
            String verifyLink = appDomain + "/user/verify-email?token=" + token;
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("username", user.getUsername());
            variables.put("verifyLink", verifyLink);
            
            emailService.sendHtmlEmail(
                user.getEmail(),
                "TradeWise - 邮箱验证",
                "tw-email-verification",
                variables
            );
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败", e);
        }
    }
    
    /**
     * 验证邮箱
     */
    public void verifyEmail(String token) {
        TwEmailVerificationToken verificationToken = emailVerificationTokenMapper.findByToken(token);
        if (verificationToken == null) {
            throw new RuntimeException("令牌无效或已过期");
        }
        
        // 更新用户邮箱验证状态
        userMapper.updateEmailVerified(verificationToken.getUserId(), true);
        
        // 标记令牌为已使用
        emailVerificationTokenMapper.markAsUsed(token);
    }
    
    /**
     * 创建管理员密码重置令牌并发送邮件
     */
    public String createAdminPasswordResetToken(String email) {
        TwAdmin admin = adminMapper.findByEmail(email);
        if (admin == null) {
            throw new RuntimeException("邮箱不存在");
        }
        
        String token = UUID.randomUUID().toString();
        
        TwAdminPasswordResetToken resetToken = new TwAdminPasswordResetToken();
        resetToken.setAdminId(admin.getId());
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetToken.setUsed(false);
        
        adminResetTokenMapper.insert(resetToken);
        
        // 发送重置密码邮件
        sendAdminPasswordResetEmail(admin, token);
        
        return token;
    }
    
    /**
     * 发送管理员密码重置邮件
     */
    private void sendAdminPasswordResetEmail(TwAdmin admin, String token) {
        try {
            String resetLink = appDomain + "/admin/reset-password?token=" + token;
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("username", admin.getUsername());
            variables.put("resetLink", resetLink);
            variables.put("isAdmin", true);
            
            emailService.sendHtmlEmail(
                admin.getEmail(),
                "TradeWise - 管理员密码重置",
                "tw-password-reset-email",
                variables
            );
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败，请稍后重试", e);
        }
    }
    
    /**
     * 重置管理员密码
     */
    public void resetAdminPassword(String token, String newPassword) {
        TwAdminPasswordResetToken resetToken = adminResetTokenMapper.findByToken(token);
        if (resetToken == null) {
            throw new RuntimeException("令牌无效或已过期");
        }
        
        // 更新密码
        adminMapper.updatePassword(resetToken.getAdminId(), passwordEncoder.encode(newPassword));
        
        // 标记令牌为已使用
        adminResetTokenMapper.markAsUsed(token);
    }
    
    /**
     * 创建用户密码重置令牌并发送邮件
     */
    public String createPasswordResetToken(String email) {
        TwUser user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("邮箱不存在");
        }
        
        String token = UUID.randomUUID().toString();
        
        TwPasswordResetToken resetToken = new TwPasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1)); // 1小时后过期
        resetToken.setUsed(false);
        
        resetTokenMapper.insert(resetToken);
        
        // 发送重置密码邮件
        sendPasswordResetEmail(user, token);
        
        return token;
    }
    
    /**
     * 发送密码重置邮件
     */
    private void sendPasswordResetEmail(TwUser user, String token) {
        try {
            String resetLink = appDomain + "/user/reset-password?token=" + token;
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("username", user.getUsername());
            variables.put("resetLink", resetLink);
            
            emailService.sendHtmlEmail(
                user.getEmail(),
                "TradeWise - 密码重置",
                "tw-password-reset-email",
                variables
            );
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败，请稍后重试", e);
        }
    }
    
    /**
     * 重置密码
     */
    public void resetPassword(String token, String newPassword) {
        TwPasswordResetToken resetToken = resetTokenMapper.findByToken(token);
        if (resetToken == null) {
            throw new RuntimeException("令牌无效或已过期");
        }
        
        // 更新密码
        userMapper.updatePassword(resetToken.getUserId(), passwordEncoder.encode(newPassword));
        
        // 标记令牌为已使用
        resetTokenMapper.markAsUsed(token);
    }
    
    /**
     * 修改管理员密码（登录后）
     */
    public void changeAdminPassword(Long adminId, String oldPassword, String newPassword) {
        TwAdmin admin = adminMapper.findById(adminId);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        
        // 更新密码
        adminMapper.updatePassword(adminId, passwordEncoder.encode(newPassword));
    }
    
    /**
     * 修改用户密码（登录后）
     */
    public void changeUserPassword(Long userId, String oldPassword, String newPassword) {
        TwUser user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        
        // 更新密码
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
    }
    
    /**
     * 获取用户信息
     */
    public TwUser getUserById(Long userId) {
        return userMapper.findById(userId);
    }
    
    /**
     * 更新管理员登录信息
     */
    public void updateAdminLoginInfo(Long adminId, String loginIp) {
        adminMapper.updateLoginInfo(adminId, loginIp);
    }
    
    /**
     * 更新用户登录信息
     */
    public void updateUserLoginInfo(Long userId, String loginIp) {
        userMapper.updateLoginInfo(userId, loginIp);
    }
}
