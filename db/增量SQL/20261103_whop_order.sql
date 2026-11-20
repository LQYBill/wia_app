-- Apply before enabling WhopOrderPullJob. All timestamps below are UTC.
CREATE TABLE IF NOT EXISTS whop_order (
    account_id varchar(64) NOT NULL,
    payment_id varchar(64) NOT NULL,
    payment_status varchar(32),
    payment_substatus varchar(64),
    sku varchar(255),
    currency varchar(16),
    total decimal(24,8),
    source_created_at varchar(40),
    source_updated_at varchar(40),
    raw_json longtext NOT NULL,
    mabang_request_json longtext,
    mabang_response_json longtext,
    push_status varchar(32) NOT NULL DEFAULT 'PENDING',
    push_attempts int NOT NULL DEFAULT 0,
    last_error text,
    created_at datetime(3) NOT NULL,
    updated_at datetime(3) NOT NULL,
    PRIMARY KEY (account_id, payment_id),
    KEY idx_whop_push (account_id, push_status, updated_at),
    KEY idx_whop_refresh (account_id, updated_at, payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS whop_sync_state (
    account_id varchar(64) NOT NULL PRIMARY KEY,
    last_completed_at varchar(40) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
