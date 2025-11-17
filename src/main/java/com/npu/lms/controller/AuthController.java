package com.npu.lms.controller;

import com.npu.lms.entity.User;
import com.npu.lms.security.JwtTokenProvider;
import com.npu.lms.security.LoginRequest;
import com.npu.lms.security.JwtResponse;
import com.npu.lms.security.RegisterRequest;
import com.npu.lms.service.UserService;
import com.npu.lms.dto.UserDTO; // 引入 UserDTO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // 1. 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 2. 放入上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. 获取 User 详情 (已实现 UserDetails)
        User user = (User) authentication.getPrincipal();

        // 4. 生成 Token
        String jwt = tokenProvider.generateToken(user);

        // --- 核心修改：手动将 User 实体数据映射到 UserDTO ---
        UserDTO userDto = new UserDTO();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        // 关键：确保 name 和 role 字段被包含
        userDto.setName(user.getName());
        userDto.setRole(user.getRole());
        // -----------------------------------------------------

        // 5. 返回 Token 和 UserDTO
        return ResponseEntity.ok(new JwtResponse(jwt, userDto));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User user = userService.registerUser(registerRequest);
            return ResponseEntity.ok("用户注册成功");
        } catch (RuntimeException e) {
            // 捕获学号重复的异常
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}