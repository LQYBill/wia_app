CREATE OR REPLACE VIEW sav_refund_with_shop_code AS
SELECT sr.*, s.erp_code
FROM sav_refund sr
         JOIN platform_order po ON sr.platform_order_id = po.id
         JOIN shop s ON po.shop_id = s.id
ORDER BY sr.create_time DESC;