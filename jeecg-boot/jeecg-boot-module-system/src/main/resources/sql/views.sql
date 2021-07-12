# creation of view popular_country
CREATE VIEW popular_country AS
SELECT country, c.name_zh, c.code, count(*) AS 'quantity'
FROM platform_order po
         JOIN country c on po.country = c.name_en
GROUP BY country
ORDER BY count(*) DESC