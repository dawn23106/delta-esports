-- 上线前先备份数据库，再在已有生产库执行一次。
ALTER TABLE t_user ADD COLUMN open_id VARCHAR(64) NULL;
ALTER TABLE t_user ADD COLUMN booster_status VARCHAR(20) NOT NULL DEFAULT 'offline';

CREATE UNIQUE INDEX uk_user_open_id ON t_user(open_id);
CREATE UNIQUE INDEX uk_settlement_order ON t_settlement(order_id);
CREATE UNIQUE INDEX uk_review_order ON t_review(order_id);

-- 性能索引（MySQL 不支持 CREATE INDEX IF NOT EXISTS，请确认不存在同名索引后再执行）
CREATE INDEX idx_user_role_status ON t_user(role, status);
CREATE INDEX idx_order_boss ON t_order(boss_id);
CREATE INDEX idx_order_booster ON t_order(booster_id);
CREATE INDEX idx_order_status ON t_order(status, created_at);
CREATE INDEX idx_order_msg_order ON t_order_message(order_id, created_at);
CREATE INDEX idx_balance_user ON t_balance_transaction(user_id, created_at);
CREATE INDEX idx_gift_sender ON t_gift(sender_id);
CREATE INDEX idx_gift_receiver ON t_gift(receiver_id);
CREATE INDEX idx_review_booster ON t_review(booster_id);
CREATE INDEX idx_review_boss ON t_review(boss_id);
CREATE INDEX idx_settlement_booster ON t_settlement(booster_id);
