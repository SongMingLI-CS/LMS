-- ============================================================
-- V6: P2 采购与资产
-- 供应商 / 采购订单 / 订单明细
-- ============================================================

CREATE TABLE suppliers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255),
    address VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_suppliers_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE purchase_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(255) NOT NULL,
    supplier_id BIGINT NOT NULL,
    order_date DATE NOT NULL,
    status VARCHAR(255) NOT NULL,
    total_amount DECIMAL(12,2),
    remark VARCHAR(1024),
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_orders_no (order_no),
    KEY idx_purchase_orders_supplier (supplier_id),
    CONSTRAINT fk_purchase_orders_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE purchase_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    isbn VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2),
    PRIMARY KEY (id),
    KEY idx_purchase_order_items_order (order_id),
    CONSTRAINT fk_purchase_order_items_order FOREIGN KEY (order_id) REFERENCES purchase_orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
