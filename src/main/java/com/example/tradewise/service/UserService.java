package com.example.tradewise.service;

import com.example.tradewise.entity.User;
import com.example.tradewise.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null || !user.getEnabled()) {
            throw new UsernameNotFoundException("用户不存在或已禁用");
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    public List<User> getAllUsers() {
        return userMapper.findAllEnabled();
    }

    public User createUser(String username, String password, String role) {
        User user = new User(username, passwordEncoder.encode(password), role, true);
        userMapper.insert(user);
        return user;
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /**
     * 用户认证
     */
    public User authenticate(String username, String password) {
        User user = userMapper.findByUsername(username);
        
        if (user == null || !user.getEnabled()) {
            return null;
        }
        
        // 验证密码
        if (passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        
        return null;
    }
}
