package com.npu.lms.service;

import com.npu.lms.dto.ChangePasswordRequest;
import com.npu.lms.dto.ProfileUpdateRequest;
import com.npu.lms.dto.UserDTO;
import com.npu.lms.entity.User;
import com.npu.lms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 辅助方法：从安全上下文中获取当前登录的用户实体
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("当前登录用户未找到: " + username));
    }

    /**
     * 获取当前用户的个人资料
     */
    public UserDTO getUserProfile() {
        User user = getCurrentUser();
        // 重用你现有的 UserDTO
        UserDTO userDto = new UserDTO();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setName(user.getName());
        userDto.setRole(user.getRole());
        return userDto;
    }

    /**
     * 更新当前用户的个人资料 (例如：姓名)
     */
    @Transactional
    public UserDTO updateUserProfile(ProfileUpdateRequest request) {
        User user = getCurrentUser();
        user.setName(request.getName());
        User updatedUser = userRepository.save(user);

        // 返回更新后的 DTO
        UserDTO userDto = new UserDTO();
        userDto.setId(updatedUser.getId());
        userDto.setUsername(updatedUser.getUsername());
        userDto.setName(updatedUser.getName());
        userDto.setRole(updatedUser.getRole());
        return userDto;
    }

    /**
     * 更改当前用户的密码
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        // 1. 验证旧密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("旧密码不正确");
        }

        // 2. 验证新密码是否为空
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            throw new BadCredentialsException("新密码不能为空");
        }

        // 3. 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}