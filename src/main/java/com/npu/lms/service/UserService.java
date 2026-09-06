package com.npu.lms.service;

import com.npu.lms.entity.User;
import com.npu.lms.repository.UserRepository;
import com.npu.lms.security.RegisterRequest;
import com.npu.lms.security.TokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List; // 引入 List
import java.util.Optional; // 引入 Optional
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TokenService tokenService;

    /**
     * 【V2 注册】创建未验证的用户并发送验证码
     */
    @Transactional
    public User registerUser(RegisterRequest registerRequest) {

        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("注册失败：该学号/用户名已被注册！");
        }
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("注册失败：该邮箱已被使用！");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole("USER"); // 默认角色
        user.setVerified(false);

        String verificationCode = String.format("%06d", new Random().nextInt(999999));
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(15));

        User savedUser = userRepository.save(user);

        String subject = "LMS 系统 - 欢迎注册";
        String body = "感谢您注册LMS系统！\n\n您的 6 位验证码是: " + verificationCode + "\n\n该验证码将在15分钟后失效。";
        emailService.sendSimpleEmail(user.getEmail(), subject, body);

        return savedUser;
    }

    /**
     * 【V2 验证】验证用户
     */
    @Transactional
    public User verifyUser(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("验证失败：未找到用户"));
        if (user.isVerified()) {
            throw new RuntimeException("验证失败：该账户已经激活");
        }
        if (user.getVerificationCodeExpiry() == null || user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("验证失败：验证码已过期，请重新注册");
        }
        if (!user.getVerificationCode().equals(code)) {
            throw new RuntimeException("验证失败：验证码错误");
        }
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        return userRepository.save(user);
    }

    // --- 【新增：UserController 所需的方法】 ---

    /**
     * 获取所有用户
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 按 ID 查找用户 (修复了 'FindById' 拼写)
     */
    public User findUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * 按用户名查找用户（供刷新令牌流程使用）
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * 更新用户信息 (Admin/Superadmin)
     */
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = findUserById(id);
        if (user == null) {
            throw new RuntimeException("未找到用户");
        }

        // 更新基础信息
        user.setName(userDetails.getName());
        user.setUsername(userDetails.getUsername());
        user.setRole(userDetails.getRole());
        user.setEmail(userDetails.getEmail()); // 确保 email 也能更新

        // 检查前端是否传入了新密码
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            // 如果传入了新密码，则加密并更新
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        // 如果密码为空，则保持数据库中的旧密码不变

        return userRepository.save(user);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    @Transactional
    public void requestPasswordReset(String email) {
        // 1. 查找用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("操作失败：该邮箱未注册"));

        // 2. 生成 6 位重置码
        String resetCode = String.format("%06d", new Random().nextInt(999999));
        user.setVerificationCode(resetCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(15)); // 15分钟后过期
        userRepository.save(user);

        // 3. 异步发送邮件
        String subject = "LMS 系统 - 密码重置请求";
        String body = "您正在请求重置LMS系统的密码。\n\n您的 6 位重置码是: " + resetCode + "\n\n该验证码将在15分钟后失效。如果您未请求此操作，请忽略此邮件。";
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    /**
     * 【新增 V2】执行密码重置
     * (由 API 2: /reset-password 调用)
     */
    @Transactional
    public void performPasswordReset(String email, String code, String newPassword) {
        // 1. 查找用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("重置失败：未找到用户"));

        // 2. 检查验证码是否过期
        if (user.getVerificationCodeExpiry() == null || user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("重置失败：验证码已过期，请重新请求");
        }

        // 3. 检查验证码是否匹配
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new RuntimeException("重置失败：验证码错误");
        }

        // 4. 验证成功：更新密码并清除验证码
        user.setPassword(passwordEncoder.encode(newPassword)); // 加密新密码
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);

        // P0: 密码重置后使旧令牌全部失效
        user.setTokenVersion(user.getTokenVersion() + 1);
        tokenService.revokeAllRefreshTokensForUser(user.getUsername());

        userRepository.save(user);
    }
}