package com.npu.lms.security;

import com.npu.lms.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        // 64+ 字节密钥，满足 HS512 要求
        ReflectionTestUtils.setField(provider, "secretString",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(provider, "accessTtlMinutes", 15L);
        provider.init();
    }

    @Test
    void generatesAndValidatesToken() {
        User user = new User();
        user.setUsername("alice");
        user.setRole("USER");
        user.setName("Alice");
        user.setTokenVersion(3L);

        String token = provider.generateAccessToken(user);

        assertTrue(provider.validateToken(token));
        assertEquals("alice", provider.getUsernameFromToken(token));
        assertEquals(3L, provider.getTokenVersionFromToken(token));
        assertNotNull(provider.getJtiFromToken(token));
        assertNotNull(provider.getExpirationFromToken(token));
    }

    @Test
    void rejectsTamperedToken() {
        User user = new User();
        user.setUsername("alice");
        user.setRole("USER");
        String token = provider.generateAccessToken(user);

        assertFalse(provider.validateToken(token + "x"));
    }
}
