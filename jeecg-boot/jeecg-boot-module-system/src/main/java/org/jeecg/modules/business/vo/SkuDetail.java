package org.jeecg.modules.business.vo;

import lombok.Data;
import org.jeecg.modules.business.entity.Promotion;
import org.jeecg.modules.business.entity.SkuPrice;

import java.util.Objects;

@Data
public class SkuDetail {

    private final String skuId;

    private final String erpCode;

    private final String product;

    private final SkuPrice price;

    private final Promotion promotion;

    public SkuDetail(String skuId, String erpCode, String product, SkuPrice price, Promotion promotion) {
        this.skuId = Objects.requireNonNull(skuId);
        this.erpCode = erpCode;
        this.product = product;
        this.price = Objects.requireNonNull(price);
        this.promotion = promotion == null ? Promotion.ZERO_PROMOTION : promotion;
    }
}
