package com.npu.lms.dto;

// (你可能需要添加 @Data (lombok) 或者手写 Getters/Setters)
public class ChangePasswordRequest {

    private String oldPassword;
    private String newPassword;

    // --- Getters and Setters ---
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}