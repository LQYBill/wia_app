CREATE OR REPLACE VIEW platform_order_logistic_expense_detail AS
SELECT s.erp_code                 AS 'shopErpCode',
       led.id,
       po.tracking_number         AS 'trackingNumber',
       po.shop_id,
       po.logistic_channel_name   AS 'logisticChannelName',
       po.platform_order_id       AS 'platformOrderId',
       po.platform_order_number   AS 'platformOrderNumber',
       po.order_time              AS 'orderTime',
       po.shipping_time           AS 'shippingTime',
       po.country,
       po.fret_fee                AS 'fretFee',
       SUM(poc.shipping_fee)      AS 'shippingFee',
       SUM(poc.vat)               AS 'vatFee',
       po.shipping_invoice_number AS 'shippingInvoiceNumber',
       led.id                     AS 'logistic_expense_detail_id',
       led.platform_order_serial_id,
       led.virtual_tracking_number,
       led.logistic_internal_number,
       led.real_weight,
       led.volumetric_weight,
       led.charging_weight,
       led.discount,
       led.shipping_fee           AS 'realShippingFee',
       led.fuel_surcharge,
       led.registration_fee,
       led.second_delivery_fee,
       led.vat,
       led.vat_service_fee,
       led.total_fee,
       led.logistic_company_id,
       led.additional_fee
FROM logistic_expense_detail led
         JOIN platform_order po ON led.tracking_number = po.tracking_number
         JOIN shop s ON po.shop_id = s.id
         JOIN platform_order_content poc ON po.id = poc.platform_order_id
GROUP BY po.id, s.erp_code
ORDER BY s.erp_code;