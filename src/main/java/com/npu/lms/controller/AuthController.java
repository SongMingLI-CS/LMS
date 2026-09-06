package com.npu.lms.controller;

import com.npu.lms.dto.PasswordResetRequest;
import com.npu.lms.dto.UserDTO;
import com.npu.lms.dto.VerificationRequest;
import com.npu.lms.entity.User;
import com.npu.lms.security.JwtResponse;
import com.npu.lms.security.JwtTokenProvider;
import com.npu.lms.security.LoginAttemptService;
import com.npu.lms.security.LoginRequest;
import com.npu.lms.security.RateLimiter;
import com.npu.lms.security.RegisterRequest;
import com.npu.lms.security.TokenService;
import com.npu.lms.service.AuditLogService;
import com.npu.lms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuditLogService auditLogService;

    // P0: 双维度限流阈值（防暴力破解 / 邮件轰炸）
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 3_600_000L;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String ip = clientIp(request);
        String username = loginRequest.getUsername() == null ? "" : loginRequest.getUsername();

        // 1. 双维度限流
        if (!rateLimiter.tryAcquire("login:ip:" + ip, 20, MINUTE)) {
            return tooMany("登录尝试过于频繁，请稍后再试");
        }
        if (!rateLimiter.tryAcquire("login:user:" + username, 10, MINUTE)) {
            return tooMany("该账号登录尝试过于频繁，请稍后再试");
        }

        // 2. 失败锁定检查（友好提示）
        if (loginAttemptService.isLocked(username)) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("message", "账户因多次登录失败已锁定，请 15 分钟后再试"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = (User) authentication.getPrincipal();

            // 检查账户是否已验证
            if (!user.isVerified()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "登录失败：账户尚未激活，请检查您的邮箱验证码"));
            }

            loginAttemptService.onLoginSuccess(username);

            // P1: 审计登录成功
            auditLogService.logAction(username, "LOGIN_SUCCESS", "登录成功", ip);

            // P0: 签发短期访问令牌 + 刷新令牌
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenService.issueRefreshToken(user);

            return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken, toDto(user)));

        } catch (BadCredentialsException e) {
            loginAttemptService.onLoginFailed(username);
            auditLogService.logAction(username, "LOGIN_FAILED", "登录失败: 密码错误", ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "账号或密码错误"));
        } catch (LockedException e) {
            auditLogService.logAction(username, "LOGIN_LOCKED", "登录被拒绝: 账户已锁定", ip);
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("message", "账户已锁定，请稍后再试"));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "账户已禁用"));
        }
    }

    // --- 【修改：注册接口 (V2 - 步骤 1: 发送验证码)】 ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        String ip = clientIp(request);
        String email = registerRequest.getEmail() == null ? "" : registerRequest.getEmail();

        // P0: 防邮件轰炸限流
        if (!rateLimiter.tryAcquire("register:ip:" + ip, 5, HOUR)) {
            return tooMany("注册请求过于频繁，请稍后再试");
        }
        if (!rateLimiter.tryAcquire("register:email:" + email, 5, HOUR)) {
            return tooMany("该邮箱注册请求过于频繁，请稍后再试");
        }

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
    public ResponseEntity<?> verifyUser(@RequestBody VerificationRequest verificationRequest, HttpServletRequest request) {
        String ip = clientIp(request);
        String username = verificationRequest.getUsername() == null ? "" : verificationRequest.getUsername();

        if (!rateLimiter.tryAcquire("verify:ip:" + ip, 10, MINUTE)) {
            return tooMany("验证请求过于频繁，请稍后再试");
        }
        if (!rateLimiter.tryAcquire("verify:user:" + username, 10, MINUTE)) {
            return tooMany("该账号验证请求过于频繁，请稍后再试");
        }

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
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String ip = clientIp(request);
        String email = payload.get("email") == null ? "" : payload.get("email");

        // P0: 防邮件轰炸限流
        if (!rateLimiter.tryAcquire("forgot:ip:" + ip, 5, HOUR)) {
            return tooMany("重置请求过于频繁，请稍后再试");
        }
        if (!rateLimiter.tryAcquire("forgot:email:" + email, 3, HOUR)) {
            return tooMany("该邮箱重置请求过于频繁，请稍后再试");
        }

        try {
            userService.requestPasswordReset(email);
            return ResponseEntity.ok(Map.of("message", "重置码已发送至您的邮箱，请在15分钟内验证。"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    // --- 【新增 V2：密码找回 (步骤 2: 验证并重置)】 ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest resetRequest, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        String email = resetRequest.getEmail() == null ? "" : resetRequest.getEmail();

        if (!rateLimiter.tryAcquire("reset:ip:" + ip, 10, MINUTE)) {
            return tooMany("重置请求过于频繁，请稍后再试");
        }
        if (!rateLimiter.tryAcquire("reset:email:" + email, 10, MINUTE)) {
            return tooMany("该邮箱重置请求过于频繁，请稍后再试");
        }

        try {
            userService.performPasswordReset(resetRequest.getEmail(), resetRequest.getCode(), resetRequest.getNewPassword());
            auditLogService.logAction(email, "PASSWORD_RESET", "密码重置成功", ip);
            return ResponseEntity.ok(Map.of("message", "密码重置成功！您现在可以使用新密码登录了。"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // --- P0: 刷新访问令牌（旋转刷新令牌） ---
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> payload) {
        String rawToken = payload.get("refreshToken");
        if (rawToken == null || rawToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "缺少刷新令牌"));
        }
        try {
            String username = tokenService.rotateRefreshToken(rawToken);
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户不存在"));
            }
            String accessToken = tokenProvider.generateAccessToken(user);
            String newRefreshToken = tokenService.issueRefreshToken(user);
            return ResponseEntity.ok(new JwtResponse(accessToken, newRefreshToken, toDto(user)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "刷新令牌无效或已过期"));
        }
    }

    // --- P0: 登出（撤销刷新令牌 + 撤销当前访问令牌 jti） ---
    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) Map<String, String> payload,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (payload != null && payload.get("refreshToken") != null) {
            tokenService.revokeRefreshToken(payload.get("refreshToken"));
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            if (tokenProvider.validateToken(jwt)) {
                tokenService.revokeAccessToken(tokenProvider.getJtiFromToken(jwt), tokenProvider.getExpirationFromToken(jwt));
            }
        }
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }

    private ResponseEntity<?> tooMany(String message) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", message));
    }

    private UserDTO toDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        return dto;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}