-- For existing Whop tables created before their collation was pinned explicitly.
-- CREATE TABLE IF NOT EXISTS does not change existing column collations.
-- Apply in the application database. These operations retain data but may rebuild/lock
-- the three Whop tables; run between scheduled executions. No platform_order/shop changes.

ALTER TABLE whop_order
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

ALTER TABLE whop_sync_state
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

ALTER TABLE whop_shipment_sync
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- Check the actual comparison columns, not only the database/table default.
SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('whop_order', 'whop_sync_state', 'whop_shipment_sync')
  AND COLLATION_NAME IS NOT NULL
ORDER BY TABLE_NAME, ORDINAL_POSITION;
