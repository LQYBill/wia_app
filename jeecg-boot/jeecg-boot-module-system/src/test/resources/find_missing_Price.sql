
# find skus that do not have price, this query should be empty
SELECT *
FROM sku
WHERE not EXISTS(
    SELECT *
    FROM sku_price
    WHERE sku_id = sku.id
    )