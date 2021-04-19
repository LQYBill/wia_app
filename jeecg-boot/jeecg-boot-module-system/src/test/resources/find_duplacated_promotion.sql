
# find skus that have multiple promotion, this should be empty
SELECT *
FROM sku
WHERE (SELECT count(*) FROM promotion_content WHERE sku_id = sku.id) > 1;