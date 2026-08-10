-- 上线前先备份数据库，再在已有生产库执行一次。
ALTER TABLE t_user ADD COLUMN open_id VARCHAR(64) NULL;
ALTER TABLE t_user ADD COLUMN booster_status VARCHAR(20) NOT NULL DEFAULT 'offline';

CREATE UNIQUE INDEX uk_user_open_id ON t_user(open_id);
CREATE UNIQUE INDEX uk_settlement_order ON t_settlement(order_id);
CREATE UNIQUE INDEX uk_review_order ON t_review(order_id);
