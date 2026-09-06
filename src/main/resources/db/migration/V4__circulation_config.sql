-- ============================================================
-- V4: P1 流通规则基础
-- 播种流通策略配置（默认值），使借期/借阅上限/续借次数/预约过期可在后台动态调整
-- 使用 INSERT IGNORE 避免与后台已手动添加的配置冲突
-- ============================================================

INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES
    ('MAX_BORROW_LIMIT', '5', '最大借阅数量'),
    ('MAX_RENEWAL_LIMIT', '1', '最大续借次数'),
    ('RESERVATION_EXPIRY_DAYS', '3', '预约取书过期天数'),
    ('LOAN_PERIOD_DAYS', '30', '默认借阅天数(天)');
