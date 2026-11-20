-- Online query indexes for shop synchronization and shipment tracking.
-- Apply once after checking SHOW INDEX FROM platform_order.
SET SESSION lock_wait_timeout = 10;
ALTER TABLE platform_order
    ADD INDEX idx_po_shop_status_order_time (shop_id, erp_status, order_time),
    ADD INDEX idx_po_channel_shipping_time (logistic_channel_name, shipping_time),
    ALGORITHM=INPLACE,
    LOCK=NONE;
