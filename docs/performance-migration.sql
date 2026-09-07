-- Delta Esports 查询性能索引（已有生产库执行一次）
-- 新环境已由 schema.sql 自动创建。执行前会先判断索引是否存在，脚本可重复运行。

DELIMITER $$
CREATE PROCEDURE add_index_if_missing(
    IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns VARCHAR(255)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = p_table
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_index_if_missing('t_user', 'idx_user_role_status_rating', '`role`, `status`, `rating`');
CALL add_index_if_missing('t_service_item', 'idx_service_active_sort', '`is_active`, `sort_order`');
CALL add_index_if_missing('t_order', 'idx_order_boss_created', '`boss_id`, `created_at`, `id`');
CALL add_index_if_missing('t_order', 'idx_order_booster_created', '`booster_id`, `created_at`, `id`');
CALL add_index_if_missing('t_order', 'idx_order_status_created', '`status`, `created_at`, `id`');
CALL add_index_if_missing('t_announcement', 'idx_announcement_sort_created', '`sort_order`, `created_at`');
CALL add_index_if_missing('t_gift', 'idx_gift_sender_created', '`sender_id`, `created_at`');
CALL add_index_if_missing('t_gift', 'idx_gift_receiver_created', '`receiver_id`, `created_at`');
CALL add_index_if_missing('t_balance_transaction', 'idx_balance_user_created', '`user_id`, `created_at`');
CALL add_index_if_missing('t_settlement', 'idx_settlement_booster_status_created', '`booster_id`, `status`, `created_at`');
CALL add_index_if_missing('t_settlement', 'idx_settlement_created', '`created_at`');
CALL add_index_if_missing('t_review', 'idx_review_booster_created', '`booster_id`, `created_at`');
CALL add_index_if_missing('t_review', 'idx_review_boss_created', '`boss_id`, `created_at`');
CALL add_index_if_missing('t_order_message', 'idx_order_msg_cursor', '`order_id`, `id`');
CALL add_index_if_missing('t_withdrawal', 'idx_withdrawal_user_created', '`user_id`, `created_at`');
CALL add_index_if_missing('t_withdrawal', 'idx_withdrawal_user_status', '`user_id`, `status`');
CALL add_index_if_missing('t_withdrawal', 'idx_withdrawal_status_created', '`status`, `created_at`');

DROP PROCEDURE add_index_if_missing;
