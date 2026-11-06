package org.jeecg.modules.business.domain.api.whop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Locale;

@Component
public class WhopOrderConverter {
    private static final Map<String, String> SKUS = Map.of(
            "PR-AURORE", "20261023JY007-TID", "PR-LILAS", "20261023JY003-TID",
            "PR-EMERAUDE", "20261023JY001-TID", "PR-JASMINE", "20261023JY002-TID",
            "PR-ROSE", "20261023JY005-TID", "PR-OPT-BATT", "20261026JY001-TID",
            "PR-MASQUE", "20261026JY002-TID", "PR-OR", "20261023JY004-TID",
            "PR-ARIEL", "20261023JY006-TID");
    private static final DateTimeFormatter PAID_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    public JSONObject convert(JSONObject payment) {
        if (!"paid".equals(payment.getString("status")) || !"succeeded".equals(payment.getString("substatus"))
                || money(payment, "refunded_amount").signum() != 0 || payment.getBooleanValue("auto_refunded")) {
            throw new IllegalArgumentException("Payment is unpaid, refunded or disputed");
        }
        if (payment.getString("shipment_id") != null) throw new IllegalArgumentException("Payment already has a shipment");
        String id = required(payment, "id");
        if (!id.matches("[A-Za-z0-9_-]{1,45}")) throw new IllegalArgumentException("Invalid external order number");
        JSONObject address = payment.getJSONObject("shipping_address");
        if (address == null) throw new IllegalArgumentException("Missing shipping address");
        JSONObject result = new JSONObject();
        result.put("platformOrderId", id);
        result.put("salesRecordNumber", id);
        result.put("shopName", "TID-Whop");
        result.put("buyerUserId", required(payment, "member_id"));
        result.put("buyerName", required(address, "name"));
        String country = required(address, "country").toUpperCase(Locale.ROOT);
        if (!country.matches("[A-Z]{2}")) throw new IllegalArgumentException("Invalid country code");
        result.put("countryCode", country);
        result.put("province", address.getString("state"));
        result.put("city", required(address, "city"));
        result.put("street1", required(address, "line1"));
        result.put("street2", address.getString("line2"));
        result.put("postCode", required(address, "postal_code"));
        result.put("phone1", payment.getString("customer_phone"));
        result.put("email", payment.getString("customer_email"));
        result.put("currencyId", required(payment, "currency").toUpperCase(Locale.ROOT));
        result.put("paidTime", PAID_TIME.format(Instant.parse(required(payment, "paid_at"))));
        result.put("paymentName", payment.getString("payment_method_type"));
        JSONArray lines = payment.getJSONArray("line_items");
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("Missing purchased line items");
        JSONArray goods = new JSONArray();
        BigDecimal itemTotal = BigDecimal.ZERO, shipping = BigDecimal.ZERO, services = BigDecimal.ZERO;
        BigDecimal linesTotal = BigDecimal.ZERO;
        StringBuilder remark = new StringBuilder("Whop " + id);
        for (int i = 0; i < lines.size(); i++) {
            JSONObject line = lines.getJSONObject(i);
            String sku = line.getString("sku");
            BigDecimal amount = money(line, "subtotal");
            if (amount.signum() < 0) throw new IllegalArgumentException("Negative line subtotal");
            linesTotal = linesTotal.add(amount);
            if (!payment.getString("currency").equalsIgnoreCase(line.getJSONObject("subtotal").getString("currency"))) {
                throw new IllegalArgumentException("Line currency mismatch");
            }
            if (sku != null && SKUS.containsKey(sku)) {
                BigDecimal quantity = line.getBigDecimal("quantity");
                if (quantity == null || quantity.signum() <= 0 || quantity.stripTrailingZeros().scale() > 0) {
                    throw new IllegalArgumentException("Invalid quantity for " + sku);
                }
                JSONObject item = new JSONObject();
                item.put("platformSku", SKUS.get(sku));
                item.put("quantity", quantity.intValueExact());
                item.put("title", required(line, "label"));
                item.put("pictureUrl", ""); // Mabang falls back to its product picture.
                item.put("itemId", required(line, "id"));
                item.put("transactionId", line.getString("id"));
                item.put("sellPrice", amount.divide(quantity, 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
                item.put("specifics", "Whop SKU: " + sku);
                goods.add(item);
                itemTotal = itemTotal.add(amount);
            } else if ("prod_1uIxsaPwKWLHB".equals(line.getString("product_id"))) {
                shipping = shipping.add(amount);
            } else if ("PR-PROTECT".equals(sku)
                    || "prod_HGvV1nKBGSr0Q".equals(line.getString("product_id"))
                    || "prod_8NhZUbgCmccYR".equals(line.getString("product_id"))) {
                services = services.add(amount);
            } else {
                throw new IllegalArgumentException("Unmapped item: " + (sku == null ? line.getString("product_id") : sku));
            }
            remark.append("; ").append(line.getString("label")).append(" x").append(line.getString("quantity"))
                    .append("=").append(amount.toPlainString());
        }
        if (goods.isEmpty()) throw new IllegalArgumentException("No mapped physical goods");
        BigDecimal total = money(payment, "total");
        BigDecimal difference = total.subtract(linesTotal);
        // Preserve the payment total exactly; a negative difference is an order discount.
        result.put("itemTotal", itemTotal.toPlainString());
        result.put("shippingFee", shipping.toPlainString());
        result.put("otherIncome", services.add(difference.max(BigDecimal.ZERO)).toPlainString());
        result.put("voucherPrice", difference.min(BigDecimal.ZERO).negate().toPlainString());
        result.put("orderItemList", goods);
        result.put("remark", remark.toString());
        return result;
    }

    public static BigDecimal money(JSONObject value, String key) {
        Object raw = value.get(key);
        if (raw == null && "refunded_amount".equals(key)) return BigDecimal.ZERO;
        if (!(raw instanceof JSONObject)) throw new IllegalArgumentException("Missing money object: " + key);
        JSONObject amount = (JSONObject) raw;
        BigDecimal number = amount.getBigDecimal("amount");
        if (number == null) throw new IllegalArgumentException("Missing money amount: " + key);
        if (value.getString("currency") != null && !value.getString("currency").equalsIgnoreCase(amount.getString("currency"))) {
            throw new IllegalArgumentException("Money currency mismatch");
        }
        return number;
    }

    private static String required(JSONObject json, String key) {
        String value = json.getString(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }
}
