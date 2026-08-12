-- 生产环境执行前请先备份数据库。
CREATE TABLE IF NOT EXISTS t_payment_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    out_trade_no VARCHAR(32) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL DEFAULT 'wechat',
    status VARCHAR(30) NOT NULL DEFAULT 'created',
    amount DECIMAL(10,2) NOT NULL,
    merchant_id VARCHAR(64),
    provider_order_no VARCHAR(100),
    provider_pay_no VARCHAR(100),
    refund_no VARCHAR(100),
    refunded_amount DECIMAL(10,2) DEFAULT 0.00,
    failure_reason VARCHAR(500),
    paid_at TIMESTAMP NULL,
    refunded_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_out_trade_no (out_trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
