-- ============================================
-- Delta Esports 数据库建表脚本
-- 沧月电竞 - 游戏陪玩代练服务平台
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'boss',
    gender VARCHAR(10),
    status VARCHAR(20) DEFAULT 'active',
    balance DECIMAL(10,2) DEFAULT 0.00,
    rating DECIMAL(3,2) DEFAULT 5.00,
    total_orders INT DEFAULT 0,
    introduction VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 服务项目表
CREATE TABLE IF NOT EXISTS t_service_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    price_unit VARCHAR(20) DEFAULT 'hour',
    cover_image VARCHAR(500),
    guarantee_desc VARCHAR(500),
    refund_policy VARCHAR(500),
    is_active TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    boss_id BIGINT NOT NULL,
    booster_id BIGINT,
    service_id BIGINT,
    service_name VARCHAR(200),
    game_region VARCHAR(50),
    game_rank VARCHAR(50),
    game_map VARCHAR(100),
    boss_note VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    amount DECIMAL(10,2) DEFAULT 0.00,
    is_qualified TINYINT,
    result_note VARCHAR(500),
    result_images TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 公告表
CREATE TABLE IF NOT EXISTS t_announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    status VARCHAR(20) DEFAULT 'published',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 礼物记录表
CREATE TABLE IF NOT EXISTS t_gift (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    gift_name VARCHAR(100) NOT NULL,
    gift_image VARCHAR(500),
    price DECIMAL(10,2) DEFAULT 0.00,
    message VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 资金流水表
CREATE TABLE IF NOT EXISTS t_balance_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    balance_after DECIMAL(10,2),
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 结算表
CREATE TABLE IF NOT EXISTS t_settlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    booster_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 评价表
CREATE TABLE IF NOT EXISTS t_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    boss_id BIGINT NOT NULL,
    booster_id BIGINT NOT NULL,
    rating INT DEFAULT 5,
    content VARCHAR(500),
    tags VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
