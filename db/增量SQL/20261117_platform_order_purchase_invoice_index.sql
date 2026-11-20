-- Supports the purchase-invoice branch of countPlatformOrders.
-- Apply once after checking SHOW INDEX FROM platform_order.
SET SESSION lock_wait_timeout = 10;
ALTER TABLE platform_order
    ADD INDEX idx_platform_order_purchase_invoice_number (purchase_invoice_number),
    ALGORITHM=INPLACE,
    LOCK=NONE;
