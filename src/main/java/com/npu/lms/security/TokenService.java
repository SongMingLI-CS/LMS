package com.npu.lms.security;

import com.npu.lms.entity.RefreshToken;
import com.npu.lms.entity.User;
import com.npu.lms.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P0 安全加固：令牌生命周期管理。
 *
 * <ul>
 *   <li>刷新令牌：数据库存储哈希，支持签发、轮换、撤销与按用户批量撤销。</li>
 *   <li>访问令牌：基于 jti 的内存撤销表，登出后在剩余有效期内拒绝该令牌。</li>
 * </ul>
 */
@Service
public class TokenService {

    @Value("${app.jwt.refresh-ttl-days:7}")
    private long refreshTtlDays;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    /** 已撤销访问令牌：jti -> 过期时间(毫秒)，用于在剩余有效期内拒绝已登出令牌 */
    private final Map<String, Long> revokedJti = new ConcurrentHashMap<>();

    /**
     * 为用户签发一枚新的刷新令牌，返回原始令牌（仅此一次可见）。
     */
    @Transactional
    public String issueRefreshToken(User user) {
        String raw = newRawToken();
        RefreshToken token = new RefreshToken();
        token.setTokenHash(sha256(raw));
        token.setUsername(user.getUsername());
        token.setExpiry(LocalDateTime.now().plusDays(refreshTtlDays));
        token.setRevoked(false);
        token.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
        return raw;
    }

    /**
     * 校验并轮换刷新令牌：撤销旧令牌，返回所属用户名。
     */
    @Transactional
    public String rotateRefreshToken(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("刷新令牌无效"));
        if (stored.isRevoked()) {
            throw new IllegalArgumentException("刷新令牌已撤销");
        }
        if (stored.getExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("刷新令牌已过期");
        }
        stored.setRevoked(true);
        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);
        return stored.getUsername();
    }

    /**
     * 撤销单枚刷新令牌（登出时调用）。
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawToken)).ifPresent(stored -> {
            if (!stored.isRevoked()) {
                stored.setRevoked(true);
                stored.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(stored);
            }
        });
    }

    /**
     * 撤销某用户的所有刷新令牌（改密/重置密码时调用，使旧会话失效）。
     */
    @Transactional
    public void revokeAllRefreshTokensForUser(String username) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUsername(username);
        for (RefreshToken token : tokens) {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedAt(LocalDateTime.now());
            }
        }
        refreshTokenRepository.saveAll(tokens);
    }

    /**
     * 将访问令牌的 jti 加入撤销表，直到其自然过期。
     */
    public void revokeAccessToken(String jti, Date expiry) {
        if (jti == null || expiry == null) {
            return;
        }
        revokedJti.put(jti, expiry.getTime());
        long now = System.currentTimeMillis();
        revokedJti.entrySet().removeIf(e -> e.getValue() < now);
    }

    /**
     * 判断访问令牌是否已被撤销。
     */
    public boolean isAccessTokenRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        Long expiry = revokedJti.get(jti);
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            revokedJti.remove(jti);
            return false;
        }
        return true;
    }

    private String newRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
