package com.npu.lms.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // 加密后的密码

    private String name; // 真实姓名或昵称

    @Column(nullable = false)
    private String role; // 角色 (例如: "ROLE_SUPERADMIN", "USER")

    // --- (V2 认证字段) ---
    @Column(unique = true)
    private String email;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expiry")
    private LocalDateTime verificationCodeExpiry;

    // --- (P0 安全: 登录失败锁定与令牌版本) ---
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion = 0;


    // --- 【修复】Getters and Setters (已全部补全) ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // (getPassword 和 getUsername 在下面 UserDetails 部分)

    // 【修复】补全 setUsername
    public void setUsername(String username) {
        this.username = username;
    }

    // 【修复】补全 setPassword
    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isVerified() { return isVerified; } // Getter 是 'isVerified'
    public void setVerified(boolean verified) { this.isVerified = verified; } // Setter 是 'setVerified'

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public LocalDateTime getVerificationCodeExpiry() { return verificationCodeExpiry; }
    public void setVerificationCodeExpiry(LocalDateTime expiryTime) { this.verificationCodeExpiry = expiryTime; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public long getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }


    // --- Spring Security UserDetails 接口实现 ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == null || this.role.isEmpty()) {
            return Collections.emptyList();
        }
        String cleanRole = this.role.toUpperCase().replace("ROLE_", "");
        String finalAuthority = "ROLE_" + cleanRole;
        return Collections.singletonList(new SimpleGrantedAuthority(finalAuthority));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() {
        // P0: 失败锁定期间账户不可登录（Spring Security 会抛出 LockedException）
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}