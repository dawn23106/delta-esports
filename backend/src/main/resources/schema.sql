-- 数据库初始化脚本 (手动执行或配置 spring.sql.init)
CREATE DATABASE IF NOT EXISTS delta_helper DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE delta_helper;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(16) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(32),
    role VARCHAR(20) NOT NULL DEFAULT 'player',
    balance DECIMAL(10,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game VARCHAR(64) NOT NULL,
    detail VARCHAR(128),
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    customer_id BIGINT NOT NULL,
    booster_id BIGINT,
    cs_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_orders_status (status),
    INDEX idx_orders_customer (customer_id),
    INDEX idx_orders_booster (booster_id),
    INDEX idx_orders_status_game (status, game)
) ENGINE=InnoDB;

