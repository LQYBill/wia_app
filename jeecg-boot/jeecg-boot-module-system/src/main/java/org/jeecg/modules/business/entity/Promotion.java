package org.jeecg.modules.business.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;


/**
 * A promotion of a sku to which it belongs, it's a immutable objet.
 */
@EqualsAndHashCode
public class Promotion {

    public static final Promotion ZERO_PROMOTION = new Promotion("-1", 1, 0, BigDecimal.ZERO);


    /**
     * Identifier of promotion
     */
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
    private final int quantityPurchased;

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
    public BigDecimal calculateDiscountAmount(int quantity) {
        int count = (quantity + quantityPurchased) / promoMilestone;
        return discount.multiply(new BigDecimal(count));
    }

    /**
     * Add purchase quantity of promotion, and return the new promotion
     *
     * @param quantity the quantity to add
     * @return the new promotion object
     */
    public Promotion addPurchaseQuantity(int quantity) {
        int new_quantity = quantityPurchased + quantity;
        if (new_quantity > promoMilestone) {
            new_quantity %= promoMilestone;
        }
        return new Promotion(id, promoMilestone, new_quantity, discount);
    }

}
