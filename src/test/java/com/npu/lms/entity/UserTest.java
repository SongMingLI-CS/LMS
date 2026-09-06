package com.npu.lms.entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void accountNotLockedByDefault() {
        User user = new User();
        assertTrue(user.isAccountNonLocked());
    }

    @Test
    void accountLockedWhenLockedUntilInFuture() {
        User user = new User();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        assertFalse(user.isAccountNonLocked());
    }

    @Test
    void accountUnlockedWhenLockExpired() {
        User user = new User();
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        assertTrue(user.isAccountNonLocked());
    }

    @Test
    void authoritiesNormalizeRole() {
        User user = new User();
        user.setRole("USER");
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
    }

    @Test
    void authoritiesHandleNullRole() {
        User user = new User();
        user.setRole(null);
        assertTrue(user.getAuthorities().isEmpty());
    }
}
