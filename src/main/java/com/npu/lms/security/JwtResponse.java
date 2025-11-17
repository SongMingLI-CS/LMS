package com.npu.lms.security;

import com.npu.lms.dto.UserDTO; // 引入您原有的 UserDTO

public class JwtResponse {
    private String token;
    private UserDTO user; // <--- 使用 UserDTO

    // Constructor
    public JwtResponse(String token, UserDTO user) { // <--- 构造函数使用 UserDTO
        this.token = token;
        this.user = user;
    }

    // Getters
    public String getToken() { return token; }
    public UserDTO getUser() { return user; }
}