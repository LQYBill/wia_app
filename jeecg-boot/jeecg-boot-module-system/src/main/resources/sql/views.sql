# creation of view popular_country
CREATE VIEW popular_country AS
SELECT country, c.name_zh, c.code, count(*) AS 'quantity'
FROM platform_order po
         JOIN country c on po.country = c.name_en
GROUP BY country
ORDER BY count(*) DESC;

# user_sku
CREATE VIEW user_sku AS
SELECT uc.user_id, spn.id AS 'sku_id', spn.erp_code, zh_name, en_name
FROM client c
         JOIN user_client uc ON c.id = uc.client_id
         JOIN client_sku cs ON c.id = cs.client_id
         JOIN sku s ON cs.sku_id = s.id
         JOIN sku_product_name spn ON s.id = spn.id
ORDER BY uc.user_id