package com.npu.lms.security;

import com.npu.lms.entity.User;
import com.npu.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginAttemptServiceTest {

    private UserRepository userRepository;
    private LoginAttemptService service;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new LoginAttemptService();
        ReflectionTestUtils.setField(service, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(service, "lockDurationMinutes", 15L);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);

        user = new User();
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    @Test
    void locksAfterMaxFailures() {
        for (int i = 0; i < 5; i++) {
            service.onLoginFailed("alice");
        }
        assertTrue(service.isLocked("alice"));
        assertNotNull(user.getLockedUntil());
    }

    @Test
    void doesNotLockBeforeMaxFailures() {
        for (int i = 0; i < 4; i++) {
            service.onLoginFailed("alice");
        }
        assertFalse(service.isLocked("alice"));
    }

    @Test
    void successResetsAttempts() {
        service.onLoginFailed("alice");
        service.onLoginFailed("alice");
        service.onLoginSuccess("alice");
        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getLockedUntil());
    }
}
