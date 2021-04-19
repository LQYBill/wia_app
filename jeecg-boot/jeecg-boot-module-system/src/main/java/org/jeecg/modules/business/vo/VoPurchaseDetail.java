package org.jeecg.modules.business.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoPurchaseDetail {
    private final String erpCode;

    private final String product;

    private final int quantity;

    private final BigDecimal price;

    private final BigDecimal total;

    public VoPurchaseDetail(String erpCode, String product, int quantity, BigDecimal price) {
        this.erpCode = erpCode;
        this.product = product;

        this.quantity = quantity;
        this.price = price;
        this.total = new BigDecimal(quantity).multiply(price);
    }
}
