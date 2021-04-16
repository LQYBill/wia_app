
# find skus that have multiple promotion
SELECT *
FROM sku
WHERE (SELECT count(*) FROM promotion_content WHERE sku_id = sku.id) > 1;