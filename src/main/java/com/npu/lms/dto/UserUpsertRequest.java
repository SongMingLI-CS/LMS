package com.npu.lms.dto;

/**
 * P0 安全：用户新增/更新的请求体。
 *
 * <p>替代原先直接把 {@code User} 实体绑定到 @RequestBody 的做法（mass assignment），
 * 防止客户端自行设置 role 之外的 tokenVersion / failedAttempts / lockedUntil /
 * verificationCode 等内部字段。</p>
 */
public class UserUpsertRequest {

    private String username;
    private String name;
    private String email;
    private String role;
    /** 新增时必填；更新时留空表示不修改密码。 */
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
