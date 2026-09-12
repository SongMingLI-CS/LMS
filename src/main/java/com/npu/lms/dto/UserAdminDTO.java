package com.npu.lms.dto;

import com.npu.lms.entity.User;

/**
 * P0 安全：用户管理接口的对外视图。
 *
 * <p>历史实现直接序列化 {@link User} 实体，导致 BCrypt 口令哈希、
 * verificationCode、lockedUntil、tokenVersion 等敏感字段泄露给客户端。
 * 该 DTO 只包含管理页面真正需要的字段。</p>
 */
public class UserAdminDTO {

    private Long id;
    private String username;
    private String name;
    private String email;
    private String role;
    private boolean verified;

    public UserAdminDTO() {
    }

    public UserAdminDTO(Long id, String username, String name, String email, String role, boolean verified) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
        this.verified = verified;
    }

    /** 实体 -> DTO 映射（不暴露任何凭据字段）。 */
    public static UserAdminDTO from(User user) {
        if (user == null) {
            return null;
        }
        return new UserAdminDTO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isVerified()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
