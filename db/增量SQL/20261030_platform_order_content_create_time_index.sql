-- Supports the rolling six-month range in SkuListMabangServiceImpl.updateSkuId().
-- Check existing indexes before applying this one-time schema change.
ALTER TABLE platform_order_content
    ADD INDEX idx_poc_create_time (create_time),
    ALGORITHM=INPLACE,
    LOCK=NONE;
