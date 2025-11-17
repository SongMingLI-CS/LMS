package com.npu.lms.repository;

import com.npu.lms.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    // 提供一个按键名查找配置的方法
    Optional<SystemConfig> findByConfigKey(String configKey);
}