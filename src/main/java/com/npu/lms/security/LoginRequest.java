package com.npu.lms.security;

// (DTO - 数据传输对象)
public class LoginRequest {
    private String username;
    private String password;

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    // Setters (用于 JSON 反序列化)
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
}