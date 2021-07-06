package org.jeecg.modules.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
public class SkuWeightDiscount {

    private final String skuId;

    private final String erpCode;

    private final Integer weight;

    private final BigDecimal discount;

    public SkuWeightDiscount(String skuId, String erpCode, Integer weight, BigDecimal discount) {
        this.skuId = Objects.requireNonNull(skuId);
        this.erpCode = erpCode;
        this.weight = weight;
        this.discount = discount;
    }

    @Override
    public String toString() {
        return String.format(
                "ID: %s, ERP: %s, weight: %s, discount: %s",
                skuId, erpCode, weight, discount
        );
    }
}
