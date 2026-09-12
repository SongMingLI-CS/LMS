package com.npu.lms.service;

import com.npu.lms.dto.UserUpsertRequest;
import com.npu.lms.entity.User;
import com.npu.lms.repository.UserRepository;
import com.npu.lms.security.RegisterRequest;
import com.npu.lms.security.TokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List; // 引入 List
import java.util.Optional; // 引入 Optional
import java.util.Random;
import java.util.Set;

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

    /** 分页大小上限，防止大响应拖垮接口 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 允许排序的字段白名单，防止任意属性排序注入 */
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "username", "name", "role", "email");

    /**
     * 用户管理页服务端检索 + 分页（替代“下载全表再由前端过滤”）。
     */
    public Page<User> searchUsers(String q, int page, int size, String sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Sort order;
        if (sort == null || sort.isBlank()) {
            order = Sort.by(Sort.Direction.ASC, "id");
        } else {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            if (!SORTABLE_FIELDS.contains(field)) {
                field = "id";
            }
            Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            order = Sort.by(direction, field);
        }
        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        return userRepository.searchUsers(keyword, PageRequest.of(safePage, safeSize, order));
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

    /** 允许写入的角色白名单（与前端 role 值一致，存储为 ROLE_ 前缀大写形式）。 */
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN", "SUPERADMIN");

    /**
     * 归一化角色：接受 "user" / "USER" / "ROLE_USER" 等形式，统一存储为 "ROLE_USER"，
     * 与数据库既有约定（NotificationService/ScheduledReportService 按 ROLE_ 前缀查询）保持一致。
     */
    private String normalizeRole(String role) {
        String clean = role == null ? "" : role.toUpperCase().replace("ROLE_", "").trim();
        if (!ALLOWED_ROLES.contains(clean)) {
            throw new RuntimeException("非法角色：" + role);
        }
        return "ROLE_" + clean;
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("用户名不能为空");
        }
    }

    /**
     * 新增用户 (仅 Superadmin)。密码必填，服务端加密。
     */
    @Transactional
    public User createUser(UserUpsertRequest request) {
        if (request == null) {
            throw new RuntimeException("请求体不能为空");
        }
        validateUsername(request.getUsername());
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new RuntimeException("新增用户必须设置初始密码");
        }
        if (userRepository.findByUsername(request.getUsername().trim()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        if (email != null && !email.isEmpty() && userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("邮箱已被使用");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setName(request.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(normalizeRole(request.getRole() == null ? "USER" : request.getRole()));
        // 管理端创建的用户视为已激活（无需邮箱验证流程）
        user.setVerified(true);
        return userRepository.save(user);
    }

    /**
     * 更新用户信息 (Admin/Superadmin)
     */
    @Transactional
    public User updateUser(Long id, UserUpsertRequest request) {
        User user = findUserById(id);
        if (user == null) {
            throw new RuntimeException("未找到用户");
        }
        if (request == null) {
            throw new RuntimeException("请求体不能为空");
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().trim().equals(user.getUsername())) {
            validateUsername(request.getUsername());
            if (userRepository.findByUsername(request.getUsername().trim()).isPresent()) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(request.getUsername().trim());
        }

        user.setName(request.getName());

        String email = request.getEmail() == null ? null : request.getEmail().trim();
        if (email != null && !email.isEmpty() && !email.equals(user.getEmail())
                && userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("邮箱已被使用");
        }
        user.setEmail(email);

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(normalizeRole(request.getRole()));
        }

        // 检查是否传入了新密码
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            // 加密并更新，同时提升令牌版本使旧会话立即失效
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setTokenVersion(user.getTokenVersion() + 1);
            tokenService.revokeAllRefreshTokensForUser(user.getUsername());
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