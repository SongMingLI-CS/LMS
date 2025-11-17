package com.npu.lms.security;

import com.npu.lms.entity.User;
import com.npu.lms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Spring Security 登录时会调用此方法
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("未找到用户: " + username));

        // User 类已实现 UserDetails 接口
        return user;
    }
}