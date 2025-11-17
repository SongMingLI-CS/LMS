package com.npu.lms.security;

import com.npu.lms.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value; // 导入 @Value
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct; // 导入 PostConstruct
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // 1. 从 application.properties 注入密钥
    @Value("${app.jwt.secret}")
    private String secretString;

    private Key key;

    // 2. 在构造函数执行后，初始化密钥
    @PostConstruct
    protected void init() {
        // 将字符串密钥转换为 Key 对象
        this.key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(secretString.getBytes()));
    }

    // 3. Token 过期时间 (例如：1天)
    private final long validityInMilliseconds = 1000 * 60 * 60 * 24;

    public String generateToken(User user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(user.getUsername()) // 用户名 (学号)
                .claim("role", user.getRole()) // 用户的角色
                .claim("name", user.getName()) // 用户姓名
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS512) // 使用 HS512 签名
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // Token 无效
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
}