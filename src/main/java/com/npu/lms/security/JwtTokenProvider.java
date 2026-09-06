package com.npu.lms.security;

import com.npu.lms.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value; // 导入 @Value
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct; // 导入 PostConstruct
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    // 1. 从 application.properties 注入密钥
    @Value("${app.jwt.secret}")
    private String secretString;

    // 2. 访问令牌有效期（分钟），默认 15 分钟（P0：短期访问令牌）
    @Value("${app.jwt.access-ttl-minutes:15}")
    private long accessTtlMinutes;

    private Key key;

    // 3. 在构造函数执行后，初始化密钥
    @PostConstruct
    protected void init() {
        // 将字符串密钥转换为 Key 对象
        this.key = Keys.hmacShaKeyFor(secretString.getBytes());
    }

    /**
     * 生成短期访问令牌（含 jti 与 tokenVersion，用于撤销与改密失效）
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTtlMinutes * 60_000L);

        return Jwts.builder()
                .setSubject(user.getUsername()) // 用户名 (学号)
                .setId(UUID.randomUUID().toString()) // jti，用于登出撤销
                .claim("role", user.getRole()) // 用户的角色
                .claim("name", user.getName()) // 用户姓名
                .claim("tv", user.getTokenVersion()) // 令牌版本，改密后失效
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS512) // 使用 HS512 签名
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token 无效
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getJtiFromToken(String token) {
        return parseClaims(token).getId();
    }

    public Date getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    public long getTokenVersionFromToken(String token) {
        Object tv = parseClaims(token).get("tv");
        return tv == null ? 0L : ((Number) tv).longValue();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}