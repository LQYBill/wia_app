CREATE OR REPLACE VIEW sku_weight_discount AS
SELECT s.id, s.erp_code, p.weight, s.shipping_discount
FROM sku s
         JOIN product p ON p.id = s.product_id;