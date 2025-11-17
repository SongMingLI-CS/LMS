package com.npu.lms.entity; // 替换为您的包名

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "users") // 建议使用复数表名
public class User implements UserDetails { // <-- 实现 UserDetails

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // username 对应学号, 确保其唯一
    private String username;
    private String name;
    private String password;
    private String role; // "USER", "ADMIN", "SUPERADMIN"


    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // --- UserDetails 接口实现 ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleFromDb = this.role; // 假设返回 "ROLE_SUPERADMIN"

        // 方案 1：如果确定数据库中总是有 ROLE_ 前缀，直接返回即可
        // return Collections.singletonList(new SimpleGrantedAuthority(roleFromDb));

        // 方案 2：更健壮的写法 (推荐，先移除所有 ROLE_，再添加一次，确保唯一性)
        String cleanRole = roleFromDb.toUpperCase().replace("ROLE_", "");
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

    // 账户是否未过期
    @Override
    public boolean isAccountNonExpired() { return true; }

    // 账户是否未锁定
    @Override
    public boolean isAccountNonLocked() { return true; }

    // 凭证是否未过期
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    // 账户是否启用
    @Override
    public boolean isEnabled() { return true; }
}