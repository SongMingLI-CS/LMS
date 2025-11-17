package com.npu.lms.controller;

import com.npu.lms.entity.SystemConfig;
import com.npu.lms.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入权限注解
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
// 【安全】确保只有超级管理员才能访问此 Controller
@PreAuthorize("hasRole('ROLE_SUPERADMIN')")
public class SystemConfigController {

    @Autowired
    private SystemConfigService configService;

    /**
     * 获取所有系统配置
     */
    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        return ResponseEntity.ok(configService.getAllConfigs());
    }

    /**
     * 更新一个配置项
     * 预期 JSON: { "value": "NewValue" }
     */
    @PutMapping("/{key}")
    public ResponseEntity<SystemConfig> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> payload) {

        String value = payload.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            SystemConfig updated = configService.updateConfig(key, value);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}