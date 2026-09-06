package com.npu.lms.security;

import com.npu.lms.dto.UserDTO; // 引入您原有的 UserDTO

public class JwtResponse {
    private String token;
    private String refreshToken; // P0: 新增刷新令牌
    private UserDTO user; // <--- 使用 UserDTO

    // Constructor
    public JwtResponse(String token, String refreshToken, UserDTO user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    // Getters
    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public UserDTO getUser() { return user; }
}