CREATE OR REPLACE VIEW api_view AS
SELECT p.country, p.third_bill_code as trackingNumber,
       (SELECT JSON_ARRAYAGG(
                       JSON_OBJECT(
                               'scanType', scan_type,
                               'scanTime', scan_time,
                               'description', description_en
                           ))
        FROM parcel_trace pt
        WHERE p.id = pt.parcel_id
        ORDER BY scan_time DESC
       ) AS traces
FROM parcel p;