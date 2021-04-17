package org.jeecg.modules.business.entity;

import lombok.Data;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.Objects;


/**
 * This class describe the relation among a sku identified by its id, its quantity,
 * and its correspondent price and promotion.
 */
@Data
public class OrderContentDetail {

    private final String skuId;

    private final Integer quantity;

    private final SkuPrice price;

    private final Promotion promotion;

    public OrderContentDetail(String skuId, Integer quantity, SkuPrice price, Promotion promotion) {
        this.skuId = Objects.requireNonNull(skuId);
        this.quantity = Objects.requireNonNull(quantity);
        this.price = Objects.requireNonNull(price);
        this.promotion = promotion == null ? Promotion.ZERO_PROMOTION : promotion;
    }

    /**
     * Calculate the reduced amount by applying the promotion to the sku.
     *
     * @return the reduced amount
     */
    public BigDecimal reducedAmount() {
        return promotion.calculateDiscountAmount(quantity);
    }

    /**
     * Calculate the total price by applying the price to the sku.
     *
     * @return the total price.
     */
    public BigDecimal totalPrice() {
        return price.getPrice(quantity).multiply(new BigDecimal(quantity));
    }


}
