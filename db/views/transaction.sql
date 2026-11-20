CREATE OR REPLACE VIEW transaction AS
WITH purchase_fee AS (
    SELECT
        po.shipping_invoice_number,
        SUM(poc.purchase_fee) AS total
    FROM platform_order_content poc
             JOIN platform_order po
                  ON po.id = poc.platform_order_id
    WHERE po.shipping_invoice_number LIKE '%-%-7%'
    GROUP BY po.shipping_invoice_number
),
     combined AS (
         -- Credit
         SELECT
             id,
             create_by,
             create_time,
             update_by,
             update_time,
             'Credit' AS type,
             client_id,
             payment_proof,
             invoice_number,
             NULL AS shipping_fee,
             NULL AS purchase_fee,
             amount,
             currency_id,
             NULL AS ordered,
             status,
             NULL AS payment_approved
         FROM credit

         UNION ALL

         -- Shipping invoice
         SELECT
             si.id,
             si.create_by,
             si.create_time,
             si.update_by,
             si.update_time,
             'Debit' AS type,
             si.client_id,
             COALESCE(
                     (
                         SELECT MAX(po.payment_document)
                         FROM purchase_order po
                         WHERE po.invoice_number = si.invoice_number
                     ),
                     NULLIF(si.payment_document, '')
             ) AS payment_proof,
             si.invoice_number,
             si.total_amount AS shipping_fee,
             pf.total AS purchase_fee,
             COALESCE(pf.total + si.total_amount, si.total_amount) AS amount,
             si.currency_id,
             NULL AS ordered,
             si.status,
             si.payment_approved
         FROM shipping_invoice si
                  LEFT JOIN purchase_fee pf
                            ON pf.shipping_invoice_number = si.invoice_number
         WHERE si.client_id IS NOT NULL
           AND si.currency_id IS NOT NULL
           AND si.currency_id <> ''

         UNION ALL

         -- Purchase order
         SELECT
             id,
             create_by,
             create_time,
             update_by,
             update_time,
             'Debit' AS type,
             client_id,
             payment_document AS payment_proof,
             invoice_number,
             NULL AS shipping_fee,
             final_amount AS purchase_fee,
             final_amount AS amount,
             currency_id,
             ordered,
             status,
             payment_approved
         FROM purchase_order
         WHERE invoice_number LIKE '%-%-1%'
           AND client_id IS NOT NULL
           AND currency_id IS NOT NULL
           AND currency_id <> ''
     )
SELECT
    c.id,
    c.create_by,
    c.create_time,
    c.update_by,
    c.update_time,
    c.type,
    c.client_id,
    c.payment_proof,
    c.invoice_number,
    c.shipping_fee,
    c.purchase_fee,
    c.amount,
    cur.code AS currency,
    c.ordered,
    c.status,
    c.payment_approved
FROM combined c
         LEFT JOIN currency cur
                   ON cur.id = c.currency_id;