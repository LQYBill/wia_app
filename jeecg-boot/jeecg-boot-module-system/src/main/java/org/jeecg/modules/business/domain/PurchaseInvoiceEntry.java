package org.jeecg.modules.business.domain;

import lombok.Getter;

import java.math.BigDecimal;

public class PurchaseInvoiceEntry {

    @Getter
    private final String sku_en_name;

    @Getter
    private final Integer quantity;

    @Getter
    private final BigDecimal unitPrice;

    @Getter
    private final BigDecimal promotionAmount;

    @Getter
    private final int promotionCount;

    public PurchaseInvoiceEntry(String sku_en_name, Integer quantity, BigDecimal unitPrice, BigDecimal promotionAmount, int promotionCount) {
        this.sku_en_name = sku_en_name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.promotionAmount = promotionAmount;
        this.promotionCount = promotionCount;
    }

    public boolean hasPromotion() {
        return promotionCount != 0;
    }

    public BigDecimal totalAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }


}
