-- Run after 20261103_whop_order.sql, before enabling WhopShipmentSyncJob.
-- Separate from whop_order.push_status: this records Mabang -> Whop tracking uploads.
CREATE TABLE IF NOT EXISTS whop_shipment_sync (
    account_id varchar(64) NOT NULL,
    payment_id varchar(64) NOT NULL,
    tracking_number varchar(255) NOT NULL,
    shipment_id varchar(64),
    sync_status varchar(32) NOT NULL,
    idempotency_key varchar(100) NOT NULL,
    request_json longtext NOT NULL,
    response_json longtext,
    attempts int NOT NULL DEFAULT 0,
    last_error text,
    last_attempt_at datetime(3) NOT NULL,
    synced_at datetime(3),
    PRIMARY KEY (account_id, payment_id),
    KEY idx_whop_shipment_retry (account_id, sync_status, last_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
