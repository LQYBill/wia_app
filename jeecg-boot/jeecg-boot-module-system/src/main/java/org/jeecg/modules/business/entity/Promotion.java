package org.jeecg.modules.business.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;


/**
 * A promotion of a sku to which it belongs
 */
public class Promotion {

    /**
     * Identifier of promotion
     */
    @Getter
    private final String id;

    /**
     * Milestone of the promotion.
     */
    @Getter
    private final int promoMilestone;

    /**
     * Purchased quantity of sku involved by this promotion.
     */
    @Getter
    private int quantityPurchased;

    /**
     * Discount amount of the promotion.
     */
    @Getter
    private final BigDecimal discount;


    /**
     * Construct a promotion by its attributes
     */
    public Promotion(String id, int promoMilestone, int quantityPurchased, BigDecimal discount) {
        this.id = id;
        this.promoMilestone = promoMilestone;
        this.quantityPurchased = quantityPurchased;
        this.discount = discount;
    }

    /**
     * Given the quantity of the sku, simulating the calculation of amount of exemption and return it.
     * This method would not change state of this promotion.
     *
     * @param quantity the quantity of sku
     * @return amount of exemption, the result will be 0 if the sku does not belong to this promotion
     */
    public BigDecimal simulateExemption(int quantity) {
        int count = (quantity + quantityPurchased) / promoMilestone;
        return discount.multiply(new BigDecimal(count));
    }

    /**
     * Given the quantity of the sku, execute the calculation of amount of exemption and return it.
     * This method changes the state of the promotion itself.
     *
     * @param quantity the quantity of sku
     * @return amount of exemption, the result will be 0 if the sku does not belong to this promotion
     */
    public BigDecimal executeExemption(int quantity) {
        // change state
        quantityPurchased += quantity;
        quantityPurchased = quantityPurchased % promoMilestone;
        return simulateExemption(quantity);
    }

}
