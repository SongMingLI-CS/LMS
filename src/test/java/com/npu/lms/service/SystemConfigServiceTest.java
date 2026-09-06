package com.npu.lms.service;

import com.npu.lms.entity.SystemConfig;
import com.npu.lms.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemConfigServiceTest {

    private SystemConfigRepository repository;
    private SystemConfigService service;

    @BeforeEach
    void setUp() {
        repository = mock(SystemConfigRepository.class);
        service = new SystemConfigService();
        ReflectionTestUtils.setField(service, "configRepository", repository);
    }

    @Test
    void loanPeriodDefaultsTo30WhenNotConfigured() {
        when(repository.findAll()).thenReturn(List.of());
        service.loadConfigCache();
        assertEquals(30, service.getLoanPeriodDays());
    }

    @Test
    void loanPeriodReadsConfiguredValue() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("LOAN_PERIOD_DAYS");
        config.setConfigValue("14");
        when(repository.findAll()).thenReturn(List.of(config));
        service.loadConfigCache();
        assertEquals(14, service.getLoanPeriodDays());
    }

    @Test
    void borrowLimitDefaultsTo5WhenNotConfigured() {
        when(repository.findAll()).thenReturn(List.of());
        service.loadConfigCache();
        assertEquals(5, service.getMaxBorrowLimit());
    }
}
