package org.jeecg.modules.business.domain.api.ws;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Geographic redaction changes presentation only; event times and outcomes are retained. */
public class WSTraceNormalizer {
    private static final Pattern HAN = Pattern.compile("\\p{IsHan}");
    private static final Pattern ORIGIN = Pattern.compile(
            "(?iu)\\b(?:people['’]?s republic of china|p\\.?r\\.?c\\.?|china|chinese|chine|chn|cn|"
            + "shenzhen|guangzhou|shanghai|beijing|dongguan|yiwu|hangzhou|ningbo|zhengzhou|"
            + "hong[ -]?kong|chengdu|wuhan|xiamen|fuzhou|quanzhou|changsha|nanjing|"
            + "qingdao|tianjin|hefei|foshan|huizhou|zhuhai|nanning|suzhou|kunming|"
            + "chongqing|jinan|wuxi|wenzhou|nanchang|shijiazhuang|xian|"
            + "can|szx|pvg|pek|pkx|hgh|cgo|hkg)\\b");
    private final List<String> extraKeywords;

    public WSTraceNormalizer(List<String> extraKeywords) {
        this.extraKeywords = extraKeywords == null ? Collections.emptyList() : extraKeywords;
    }

    public String description(String value) {
        if (value == null || value.trim().isEmpty()) return "Shipment update";
        // Translate familiar events before redacting remaining geographic references.
        String text = value.trim()
                .replace("派送失败", "Delivery attempt unsuccessful")
                .replace("投递失败", "Delivery attempt unsuccessful")
                .replace("签收失败", "Delivery attempt unsuccessful")
                .replace("拒收", "Delivery refused")
                .replace("退回发件人", "Returned to sender")
                .replace("退回", "Returned to sender")
                .replace("退件", "Returned to sender")
                .replace("订单已取消", "Shipment cancelled")
                .replace("订单已创建", "Order created")
                .replace("仓库已收货", "Received at logistics facility")
                .replace("仓库操作完成，已安排出库", "Processed at logistics facility; dispatch arranged")
                .replace("航班已配载", "Assigned to transport")
                .replace("送仓报关", "Submitted for customs clearance")
                .replace("报关已放行", "Customs clearance completed")
                .replace("货站收货", "Received at cargo terminal")
                .replace("航班起飞", "Transport departed")
                .replace("航班落地", "Transport arrived")
                .replace("已通知清关行提货", "Clearance agent notified for collection")
                .replace("清关完成", "Customs clearance completed")
                .replace("已签收", "Delivered successfully")
                .replace("派送中", "Out for delivery");
        text = geographic(text);
        if (HAN.matcher(text).find()) {
            // Preserve known outcomes even in an otherwise unrecognised Chinese sentence.
            if (text.contains("Delivery attempt unsuccessful")) return "Delivery attempt unsuccessful";
            if (text.contains("Delivery refused")) return "Delivery refused";
            if (text.contains("Returned to sender")) return "Returned to sender";
            if (text.contains("Shipment cancelled")) return "Shipment cancelled";
            if (text.contains("Delivered successfully")) return "Delivered successfully";
            return "Shipment update";
        }
        return fitDatabaseText(text);
    }

    public String location(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String text = geographic(value.trim());
        return fitDatabaseText(HAN.matcher(text).find() ? "Logistics facility" : text);
    }

    public static String fitDatabaseText(String value) {
        if (value == null || value.codePointCount(0, value.length()) <= 256) return value;
        return value.substring(0, value.offsetByCodePoints(0, 255)) + "…";
    }

    private String geographic(String text) {
        text = text.replaceAll("(?iu)\\bchina\\s+post\\b", "Postal carrier");
        text = ORIGIN.matcher(text).replaceAll("Origin location");
        for (String keyword : extraKeywords) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                text = text.replaceAll("(?iu)" + Pattern.quote(keyword.trim()), "Origin location");
            }
        }
        return fitDatabaseText(text);
    }

    public String scanType(String original, Integer status) {
        String text = original == null ? "" : original.toLowerCase(Locale.ROOT);
        // Negative outcomes take precedence over a delivered substring.
        if (text.contains("refused") || text.contains("return") || text.contains("退") || text.contains("拒收")) return "Return";
        if (text.contains("fail") || text.contains("unsuccessful") || text.contains("not delivered")
                || text.contains("exception") || text.contains("cancel") || text.contains("失败") || text.contains("异常")) return "End Abnormal";
        if (Integer.valueOf(8).equals(status) || text.contains("delivered successfully")
                || text.equals("delivered") || text.contains("已签收")) return "End Delivered";
        if (text.contains("out for delivery") || text.contains("派送中")) return "End Delivery";
        if (text.contains("clearance completed") || text.contains("清关完成") || text.contains("报关已放行")) return "Customs Clearance Completed";
        if (text.contains("报关") || text.contains("清关")) return "Customs Clearance";
        if (text.contains("订单已创建") || text.contains("order created")) return "Order Placed";
        if (text.contains("航班起飞")) return "Flight Departure";
        if (text.contains("航班落地")) return "Flight Arrived";
        if (text.contains("仓库已收货")) return "Received by Consolidation Warehouse";
        if (text.contains("已安排出库")) return "Consolidation Center Dispatch";
        return "In Transit";
    }
}
