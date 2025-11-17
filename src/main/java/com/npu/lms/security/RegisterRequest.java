package com.npu.lms.security;

// (DTO - 数据传输对象)
public class RegisterRequest {
    private String username;
    private String name;
    private String password;

    // Getters
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getPassword() { return password; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setName(String name) { this.name = name; }
    public void setPassword(String password) { this.password = password; }
}