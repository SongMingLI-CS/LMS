package com.npu.lms.dto;

public class VerificationRequest {

    private String username; // 用户名或学号
    private String code;     // 6位验证码

    // --- Getters and Setters ---

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}