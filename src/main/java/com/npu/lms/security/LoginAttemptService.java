package com.npu.lms.security;

import com.npu.lms.entity.User;
import com.npu.lms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * P0 安全加固：登录失败锁定。
 *
 * <p>连续失败达到阈值后锁定账户一段时间，阻止暴力破解；成功登录后重置计数。</p>
 */
@Service
public class LoginAttemptService {

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes:15}")
    private long lockDurationMinutes;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void onLoginFailed(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedAttempts() + 1;
            if (attempts >= maxFailedAttempts) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
                user.setFailedAttempts(0);
            } else {
                user.setFailedAttempts(attempts);
            }
            userRepository.save(user);
        });
    }

    @Transactional
    public void onLoginSuccess(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        });
    }

    public boolean isLocked(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    public LocalDateTime getLockedUntil(String username) {
        return userRepository.findByUsername(username)
                .map(User::getLockedUntil)
                .orElse(null);
    }
}
