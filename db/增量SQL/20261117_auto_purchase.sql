-- Apply before enabling AutoPurchaseJob. Existing SKUs remain opted out.
ALTER TABLE sku ADD COLUMN auto_purchase_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '允许自动采购';

CREATE TABLE auto_purchase_run (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    client_id VARCHAR(36) NOT NULL,
    run_date DATE NOT NULL,
    purchase_id VARCHAR(36) NOT NULL,
    recipient VARCHAR(254) NOT NULL,
    mabang_username VARCHAR(100) NOT NULL,
    auto_mark_paid TINYINT(1) NOT NULL DEFAULT 0,
    email_sent TINYINT(1) NOT NULL DEFAULT 0,
    plan_state VARCHAR(20) NOT NULL DEFAULT 'READY',
    plan_result TEXT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auto_purchase_client_day (client_id, run_date),
    UNIQUE KEY uk_auto_purchase_order (purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
