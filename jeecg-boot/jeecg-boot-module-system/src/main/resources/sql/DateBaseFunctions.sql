CREATE FUNCTION shopErpToId(erp varchar(32))
    RETURNS varchar(36) READS SQL DATA
BEGIN
    RETURN (
        SELECT id
        FROM shop
        WHERE erp_code = erp
    );
END;

CREATE FUNCTION skuErpToId(erp varchar(32))
    RETURNS varchar(36) READS SQL DATA
BEGIN
    RETURN (
        SELECT id
        FROM sku
        WHERE erp_code = erp
    );
END;