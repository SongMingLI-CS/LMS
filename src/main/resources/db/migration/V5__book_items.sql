-- ============================================================
-- V5: P1 馆藏建模
-- 新增分馆(branches)与馆藏单册(book_items)，将书目与实体馆藏分离
-- ============================================================

CREATE TABLE branches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_branches_name (name),
    UNIQUE KEY uk_branches_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE book_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    barcode VARCHAR(255) NOT NULL,
    book_id BIGINT NOT NULL,
    branch_id BIGINT,
    shelf_no VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    acquired_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_items_barcode (barcode),
    KEY idx_book_items_book (book_id),
    KEY idx_book_items_branch (branch_id),
    CONSTRAINT fk_book_items_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT fk_book_items_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 播种默认分馆
INSERT INTO branches (name, code, address) VALUES ('主馆', 'MAIN', '默认馆舍');
