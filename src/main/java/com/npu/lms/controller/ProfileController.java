package com.npu.lms.controller;

import com.npu.lms.dto.ChangePasswordRequest;
import com.npu.lms.dto.ProfileUpdateRequest;
import com.npu.lms.dto.UserDTO;
import com.npu.lms.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile") // 专门用于个人资料的 API 路径
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    /**
     * 获取当前登录用户的个人资料
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getUserProfile() {
        return ResponseEntity.ok(profileService.getUserProfile());
    }

    /**
     * 更新当前登录用户的个人资料 (例如：姓名)
     */
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateUserProfile(@RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateUserProfile(request));
    }

    /**
     * 更改当前登录用户的密码
     */
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            profileService.changePassword(request);
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}