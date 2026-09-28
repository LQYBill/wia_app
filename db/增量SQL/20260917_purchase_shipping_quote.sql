-- client.small_purchase_shipping_fee_enabled and purchase_order.domestic_shipping_fee
-- have already been added by the operator. Do not add them again.
-- domestic_shipping_fee stores EUR, matching the purchase product/discount calculation.
CREATE TABLE IF NOT EXISTS purchase_shipping_quote (
    id varchar(36) NOT NULL,
    client_id varchar(64) NOT NULL,
    snapshot_json longtext NOT NULL,
    expires_at datetime(3) NOT NULL,
    purchase_order_id varchar(64) DEFAULT NULL,
    create_time datetime(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_shipping_quote_order (purchase_order_id),
    KEY idx_shipping_quote_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Optional scheduled housekeeping: delete ONLY expired, unconsumed quotes.
-- DELETE FROM purchase_shipping_quote WHERE purchase_order_id IS NULL
--   AND expires_at < DATE_SUB(NOW(), INTERVAL 1 DAY);
