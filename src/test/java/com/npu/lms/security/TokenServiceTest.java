package com.npu.lms.security;

import com.npu.lms.entity.RefreshToken;
import com.npu.lms.entity.User;
import com.npu.lms.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private RefreshTokenRepository repository;
    private TokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        service = new TokenService();
        ReflectionTestUtils.setField(service, "refreshTtlDays", 7L);
        ReflectionTestUtils.setField(service, "refreshTokenRepository", repository);
    }

    @Test
    void issueRefreshTokenStoresHashNotRaw() {
        User user = new User();
        user.setUsername("alice");

        String raw = service.issueRefreshToken(user);

        assertNotNull(raw);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertEquals("alice", saved.getUsername());
        assertNotEquals(raw, saved.getTokenHash());
        assertEquals(64, saved.getTokenHash().length());
    }

    @Test
    void rotateRefreshTokenReturnsUsernameAndRevokes() {
        RefreshToken stored = new RefreshToken();
        stored.setUsername("alice");
        stored.setRevoked(false);
        stored.setExpiry(LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        String username = service.rotateRefreshToken("raw-token");

        assertEquals("alice", username);
        assertTrue(stored.isRevoked());
        verify(repository).save(stored);
    }

    @Test
    void rotateRevokedTokenThrows() {
        RefreshToken stored = new RefreshToken();
        stored.setUsername("alice");
        stored.setRevoked(true);
        stored.setExpiry(LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThrows(IllegalArgumentException.class, () -> service.rotateRefreshToken("raw-token"));
    }

    @Test
    void revokeAllRefreshTokensForUser() {
        RefreshToken t1 = new RefreshToken();
        t1.setRevoked(false);
        RefreshToken t2 = new RefreshToken();
        t2.setRevoked(false);
        when(repository.findAllByUsername("alice")).thenReturn(List.of(t1, t2));

        service.revokeAllRefreshTokensForUser("alice");

        assertTrue(t1.isRevoked());
        assertTrue(t2.isRevoked());
    }

    @Test
    void accessTokenRevocation() {
        String jti = "jti-123";
        Date expiry = new Date(System.currentTimeMillis() + 60_000);

        assertFalse(service.isAccessTokenRevoked(jti));
        service.revokeAccessToken(jti, expiry);
        assertTrue(service.isAccessTokenRevoked(jti));
    }
}
