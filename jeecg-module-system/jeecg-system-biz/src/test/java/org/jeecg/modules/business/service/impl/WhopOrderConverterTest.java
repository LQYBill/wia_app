package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.api.whop.WhopOrderConverter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WhopOrderConverterTest {
    private final WhopOrderConverter converter = new WhopOrderConverter();

    @Test void mapsMultipleSkusAndQuantitiesAndPreservesAllCharges() {
        JSONObject order = payment();
        JSONArray lines = order.getJSONArray("line_items");
        lines.add(line("PR-PROTECT", "prod_HGvV1nKBGSr0Q", 1, "4.95"));
        lines.add(line(null, "prod_8NhZUbgCmccYR", 1, "9.95"));
        lines.add(line(null, "prod_1uIxsaPwKWLHB", 1, "4.90"));
        order.put("total", money("219.45")); // 198 physical + 14.90 service + 4.90 shipping + 1.65 tax.
        JSONObject result = converter.convert(order);
        assertEquals("TID-Whop", result.getString("shopName"));
        JSONArray goods = result.getJSONArray("orderItemList");
        assertEquals(2, goods.size());
        assertEquals("20261023JY007-TID", goods.getJSONObject(0).getString("platformSku"));
        assertEquals(2, goods.getJSONObject(0).getIntValue("quantity"));
        assertEquals("87", goods.getJSONObject(0).getString("sellPrice"));
        assertEquals("20261026JY001-TID", goods.getJSONObject(1).getString("platformSku"));
        assertEquals(new BigDecimal("198"), result.getBigDecimal("itemTotal"));
        BigDecimal total = result.getBigDecimal("itemTotal").add(result.getBigDecimal("shippingFee"))
                .add(result.getBigDecimal("otherIncome")).subtract(result.getBigDecimal("voucherPrice"));
        assertEquals(0, total.compareTo(new BigDecimal("219.45")));
        assertEquals("2026-11-01 20:00:00", result.getString("paidTime"));
    }

    @Test void unknownItemsBlockTheEntireOrder() {
        JSONObject payment = payment();
        payment.getJSONArray("line_items").add(line("NEW-SKU", "prod_new", 1, "10"));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(payment));
    }

    @Test void rejectsRefundsUnpaidOrdersMissingAddressesAndFractionalQuantities() {
        JSONObject refunded = payment();
        refunded.put("refunded_amount", money("1"));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(refunded));
        JSONObject unpaid = payment(); unpaid.put("status", "open");
        assertThrows(IllegalArgumentException.class, () -> converter.convert(unpaid));
        JSONObject missingAddress = payment(); missingAddress.remove("shipping_address");
        assertThrows(IllegalArgumentException.class, () -> converter.convert(missingAddress));
        JSONObject fractional = payment(); fractional.getJSONArray("line_items").getJSONObject(0).put("quantity", 1.5);
        assertThrows(IllegalArgumentException.class, () -> converter.convert(fractional));
    }

    @Test void retainsDiscountsAndDoesNotUseNeedsTrackingAsPhysicalGoodsFlag() {
        JSONObject payment = payment();
        payment.put("needs_tracking", false); // Real account returns false even for its physical orders.
        payment.put("total", money("190"));
        JSONObject result = converter.convert(payment);
        assertEquals(0, new BigDecimal("8").compareTo(result.getBigDecimal("voucherPrice")));
    }

    static JSONObject payment() {
        JSONObject payment = JSON.parseObject("{\"id\":\"pay_test\",\"account_id\":\"biz_test\",\"status\":\"paid\","
                + "\"substatus\":\"succeeded\",\"member_id\":\"mber_test\",\"currency\":\"eur\","
                + "\"paid_at\":\"2026-11-01T12:00:00Z\",\"shipping_address\":{\"name\":\"Test\",\"country\":\"FR\","
                + "\"city\":\"Paris\",\"line1\":\"Test Street\",\"postal_code\":\"75001\"}}");
        JSONArray lines = new JSONArray();
        lines.add(line("PR-AURORE", "prod_robe", 2, "174"));
        lines.add(line("PR-OPT-BATT", "prod_battery", 1, "24"));
        payment.put("line_items", lines);
        payment.put("total", money("198"));
        return payment;
    }

    private static JSONObject line(String sku, String product, int quantity, String subtotal) {
        JSONObject line = new JSONObject();
        line.put("id", "li_" + product); line.put("sku", sku); line.put("product_id", product);
        line.put("label", product); line.put("quantity", quantity); line.put("subtotal", money(subtotal));
        return line;
    }

    private static JSONObject money(String amount) {
        JSONObject money = new JSONObject(); money.put("amount", amount); money.put("currency", "eur"); return money;
    }
}
