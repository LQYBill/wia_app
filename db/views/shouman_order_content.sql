CREATE OR REPLACE VIEW shouman_order_content AS
select sh.erp_code,
       po.platform_order_id,
       po.platform_order_number,
       po.postcode,
       po.recipient,
       po.city,
       c.name_zh,
       po.shipping_label_url,
       po.tracking_number,
       lc.zh_name as logistic_channel_name,
       lc.code    as logistic_channel_code,
       poc.id     as platform_order_content_id,
       poc.sku_id,
       poc.customization_data,
       poc.customization_url,
       poc.quantity,
       sc.product_name,
       ssr.remark,
       sc.price,
       sc.image_url,
       sc.sku,
       sc.is_necklace,
       sc.is_gem,
       sc.is_memo,
       sr.content_rec_regex,
       sr.content_ext_regex,
       sr.prefix,
       sr.is_size_regex,
       sr.is_month_regex,
       sr.is_comma_separated,
       sr.is_in_inches,
       sr.use_on_buyer_message
from platform_order_content poc
         join platform_order po on poc.platform_order_id = po.id
         left join logistic_channel lc on po.logistic_channel_name = lc.zh_name
         join shop sh on po.shop_id = sh.id
         join sku s on poc.sku_id = s.id
         join shouman_sku_relation ssr on s.id = ssr.sku_id
         join shouman_category sc on ssr.shouman_category_id = sc.id
         join shouman_regex sr on sc.id = sr.shouman_category_id
         join country c on po.country = c.name_en
where po.order_time > '2025-10-01'
  and poc.erp_status in (1)
  and sr.is_active = 1
  and po.platform_order_id not in (select platform_order_id from shouman_order)
order by sc.id, sr.priority;