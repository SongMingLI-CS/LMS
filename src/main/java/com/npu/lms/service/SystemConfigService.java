package com.npu.lms.service;

import com.npu.lms.entity.SystemConfig;
import com.npu.lms.repository.SystemConfigRepository;
import jakarta.annotation.PostConstruct; // 引入 PostConstruct
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // 引入线程安全的 Map
import java.util.stream.Collectors;

@Service
public class SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    @Autowired
    private SystemConfigRepository configRepository;

    // 1. 使用线程安全的 ConcurrentHashMap 作为内存缓存
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    /**
     * @PostConstruct 注解确保此方法在 Spring Boot 应用启动时自动执行
     * 它会从数据库加载所有配置到缓存中
     */
    @PostConstruct
    public void loadConfigCache() {
        log.info("【系统配置】开始加载配置到缓存...");
        List<SystemConfig> configs = configRepository.findAll();
        for (SystemConfig config : configs) {
            configCache.put(config.getConfigKey(), config.getConfigValue());
        }
        log.info("【系统配置】加载完成。共 {} 条配置。", configs.size());
    }

    // --- 2. 提供获取具体配置的 Getter 方法 (带默认值) ---

    public int getMaxBorrowLimit() {
        // 从缓存中读取, 如果不存在则提供一个安全的默认值 5
        return Integer.parseInt(configCache.getOrDefault("MAX_BORROW_LIMIT", "5"));
    }

    public int getMaxRenewalLimit() {
        return Integer.parseInt(configCache.getOrDefault("MAX_RENEWAL_LIMIT", "1"));
    }

    public int getReservationExpiryDays() {
        return Integer.parseInt(configCache.getOrDefault("RESERVATION_EXPIRY_DAYS", "3"));
    }

    public int getLoanPeriodDays() {
        return Integer.parseInt(configCache.getOrDefault("LOAN_PERIOD_DAYS", "30"));
    }

    // --- 3. 提供给 Controller 使用的方法 ---

    /**
     * 获取所有配置列表 (供超级管理员页面显示)
     */
    public List<SystemConfig> getAllConfigs() {
        // 直接从数据库读取，确保数据最新
        return configRepository.findAll();
    }

    /**
     * 更新配置 (供超级管理员使用)
     */
    @Transactional
    public SystemConfig updateConfig(String key, String value) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("未找到配置项: " + key));

        // 1. 更新数据库
        config.setConfigValue(value);
        SystemConfig updatedConfig = configRepository.save(config);

        // 2. 更新缓存
        configCache.put(key, value);
        log.info("【系统配置】配置项 {} 已更新为: {}", key, value);

        return updatedConfig;
    }
}