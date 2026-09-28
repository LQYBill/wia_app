package org.jeecg.modules.business.vo.clientPurchaseOrder;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Stored server-side quote. Monetary components are EUR; payableAmount is in currency. */
@Data
public class PurchaseShippingQuote {
    private String quoteId;
    private long expiresAt;
    private boolean enabled;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal merchandiseAmount;
    private BigDecimal discountAmount;
    private BigDecimal domesticShippingFee;
    private BigDecimal finalAmount;
    private BigDecimal payableAmount;
    private Map<String, Integer> quantities;
    private Map<String, String> erpCodes;
    private Map<String, String> suppliers;
    private List<Group> groups;

    @Data
    public static class Group {
        private String supplier;
        private long quantity;
        private int threshold;
        private BigDecimal standardFee;
        private BigDecimal fee;
    }
}
