package com.npu.lms.repository;

import com.npu.lms.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // JpaRepository 已经提供了我们需要的分页 findAll()
}