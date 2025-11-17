package com.npu.lms.service;

import com.npu.lms.entity.User;
import com.npu.lms.repository.UserRepository;
import com.npu.lms.security.RegisterRequest;
import com.npu.lms.dto.UserDTO; // <-- NEW
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors; // <-- NEW

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- NEW: Conversion method ---
    private UserDTO convertToDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        // 确保不会暴露密码
        return dto;
    }
    // ----------------------------


    public User registerUser(RegisterRequest registerRequest) {
        // 检查学号是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            // (Controller 将捕获此异常)
            throw new RuntimeException("注册失败：该学号已被注册！");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setName(registerRequest.getName());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword())); // 密码加密
        user.setRole("USER"); // 默认注册为普通用户

        return userRepository.save(user);
    }

    // MODIFIED: 返回 DTO List
    public List<UserDTO> findAllUsersDTO() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Original saveUser logic remains
    public User saveUser(User user) {
        if (user.getId() == null || (user.getPassword() != null && !user.getPassword().isEmpty())) {
            // 假设如果密码字段不为空，就是需要更新
            if (user.getId() == null || !passwordEncoder.matches(user.getPassword(), userRepository.findById(user.getId()).orElse(user).getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        } else {
            // 如果密码字段为空 (前端编辑时)，则保留原密码
            if(user.getId() != null) {
                User oldUser = userRepository.findById(user.getId()).orElse(null);
                if (oldUser != null) {
                    user.setPassword(oldUser.getPassword());
                }
            }
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}