package org.jeecg.modules.business.vo.clientPlatformOrder.section;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Each row of client platform order page in confirmation page
 */
@Data
public class PurchaseDetail {
    private final String skuId;

    private final String erpCode;

    private final String product;

    private final int quantity;

    private final BigDecimal price;

    private final BigDecimal total;

    public PurchaseDetail(String skuId, String erpCode, String product, int quantity, BigDecimal price) {
        this.skuId = skuId;
        this.erpCode = erpCode;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.total = new BigDecimal(quantity).multiply(price);
    }
}
