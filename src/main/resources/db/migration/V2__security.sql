-- ============================================================
-- V2: P0 安全加固
-- 1) users 表新增：登录失败计数、锁定时间、令牌版本
-- 2) 新增 refresh_tokens 表：刷新令牌（存哈希，支持轮换与撤销）
-- ============================================================

ALTER TABLE users
    ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME(6) NULL,
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL,
    username VARCHAR(255) NOT NULL,
    expiry DATETIME(6) NOT NULL,
    revoked BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
