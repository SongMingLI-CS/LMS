package com.npu.lms.service;

import com.npu.lms.entity.AuditLog;
import com.npu.lms.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async; // 导入 @Async
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * 【V3】异步保存日志
     * (这需要 @EnableAsync 在你的主启动类上)
     */
    @Async
    public void logAction(String username, String action, String details, String ipAddress) {
        try {
            AuditLog log = new AuditLog(username, action, details, ipAddress);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // 异步任务中，我们只打印错误，不抛出异常
            System.err.println("保存审计日志失败: " + e.getMessage());
        }
    }
}