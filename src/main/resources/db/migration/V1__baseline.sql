-- ============================================================
-- V1: 基线 schema（与现有 JPA 实体一一对应）
-- 用途：全新数据库的完整建表；已有数据库将由 Flyway baseline 跳过本脚本
-- ============================================================

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    is_verified BIT(1) NOT NULL DEFAULT b'0',
    verification_code VARCHAR(255),
    verification_code_expiry DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(255) NOT NULL,
    stock INT NOT NULL,
    available INT NOT NULL,
    cover VARCHAR(255),
    category VARCHAR(255),
    publisher VARCHAR(255),
    publication_date DATE,
    status VARCHAR(50),
    price DECIMAL(10,2),
    supplier VARCHAR(255),
    introduction VARCHAR(2048),
    PRIMARY KEY (id),
    UNIQUE KEY uk_books_isbn (isbn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE borrow_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reservation_expiry_date DATE,
    renewal_count INT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrow_time DATETIME(6),
    due_date DATE,
    return_time DATETIME(6),
    status VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_borrow_user (user_id),
    KEY idx_borrow_book (book_id),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    details VARCHAR(1024),
    ip_address VARCHAR(255),
    `timestamp` DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE book_batches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    supplier VARCHAR(255),
    price DECIMAL(10,2),
    purchase_date DATE,
    PRIMARY KEY (id),
    KEY idx_batch_book (book_id),
    CONSTRAINT fk_batch_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE system_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(255) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
