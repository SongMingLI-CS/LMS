package com.npu.lms.controller;

import com.npu.lms.dto.PasswordResetRequest;
import com.npu.lms.entity.User;
import com.npu.lms.security.JwtTokenProvider;
import com.npu.lms.security.LoginRequest;
import com.npu.lms.security.JwtResponse;
import com.npu.lms.security.RegisterRequest;
import com.npu.lms.service.UserService;
import com.npu.lms.dto.UserDTO; // 引入 UserDTO
import com.npu.lms.dto.VerificationRequest; // 1. 引入 VerificationRequest

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

import java.util.Map; // 引入 Map

@RestController
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    // --- (登录接口保持不变) ---
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

        // 4. 【V2 检查】检查账户是否已验证
        if (!user.isVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "登录失败：账户尚未激活，请检查您的邮箱验证码"));
        }

        // 5. 生成 Token
        String jwt = tokenProvider.generateToken(user);

        // 6. 映射到 DTO
        UserDTO userDto = new UserDTO();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setName(user.getName());
        userDto.setRole(user.getRole());

        // 7. 返回 Token 和 UserDTO
        return ResponseEntity.ok(new JwtResponse(jwt, userDto));
    }

    // --- 【修改：注册接口 (V2 - 步骤 1: 发送验证码)】 ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            userService.registerUser(registerRequest);
            // 注册请求现在只负责发送邮件，不直接创建可用账户
            return ResponseEntity.ok(Map.of("message", "注册请求成功，验证码已发送至您的邮箱，请在15分钟内验证。"));
        } catch (RuntimeException e) {
            // 捕获学号/邮箱重复的异常
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    // --- 【新增：注册接口 (V2 - 步骤 2: 验证激活)】 ---
    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerificationRequest verificationRequest) {
        try {
            userService.verifyUser(verificationRequest.getUsername(), verificationRequest.getCode());
            return ResponseEntity.ok(Map.of("message", "账户激活成功！您现在可以登录了。"));
        } catch (RuntimeException e) {
            // 捕获验证码错误、过期或用户不存在的异常
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
    // --- 【新增 V2：密码找回 (步骤 1: 发送重置码)】 ---
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            userService.requestPasswordReset(email);
            return ResponseEntity.ok(Map.of("message", "重置码已发送至您的邮箱，请在15分钟内验证。"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    // --- 【新增 V2：密码找回 (步骤 2: 验证并重置)】 ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            userService.performPasswordReset(request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "密码重置成功！您现在可以使用新密码登录了。"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
}