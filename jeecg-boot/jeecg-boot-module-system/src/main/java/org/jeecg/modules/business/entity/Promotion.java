package org.jeecg.modules.business.entity;

import java.math.BigDecimal;
import java.util.*;


/**
 * A promotion has attribute
 */
public class Promotion {
    /**
     * Milestone of the promotion.
     */
    private final int promoMilestone;

    /**
     * Purchased quantity of sku involved by this promotion.
     */
    private int quantityPurchased;

    /**
     * Discount amount of the promotion.
     */
    private final BigDecimal discount;

    /**
     * Identifiers of sku involved
     */
    private final Set<String> skuIds;

    /**
     * Private constructor
     */
    private Promotion(int promoMilestone, int quantityPurchased, BigDecimal discount, Collection<String> skuIds) {
        this.promoMilestone = promoMilestone;
        this.quantityPurchased = quantityPurchased;
        this.discount = discount;
        this.skuIds = new HashSet<>(skuIds);
    }

    /**
     * Construct a promotion by indicate its promotion milestone, quantity purchased, discount amount,
     * and a string formed by concatenating the involved skus identifiers, these identifiers should be
     * separated by commas.
     *
     * @param promoMilestone    the promotion milestone
     * @param quantityPurchased the purchased quantity of skus
     * @param discount          the discount amount
     * @param skuIds            identifiers formed by concatenating the involved skus identifiers and separated by commas
     */
    public Promotion(int promoMilestone, int quantityPurchased, BigDecimal discount, String skuIds) {
        this(promoMilestone, quantityPurchased, discount, skuIds, ",");
    }

    /**
     * Construct a promotion by indicate its promotion milestone, quantity purchased, discount amount,
     * and a string formed by concatenating the involved skus identifiers, these identifiers should be
     * separated by the given regex.
     *
     * @param promoMilestone    the promotion milestone
     * @param quantityPurchased the purchased quantity of skus
     * @param discount          the discount amount
     * @param skuIds            identifiers formed by concatenating the involved skus identifiers
     *                          and separated by the given regex
     */
    public Promotion(int promoMilestone, int quantityPurchased, BigDecimal discount, String skuIds, String regex) {
        this(promoMilestone, quantityPurchased, discount, Arrays.asList(skuIds.split(regex)));
    }

    /**
     * Given the sku id and the quantity of the sku, simulating the calculation of amount of exemption and return it.
     * This method would not change state of this promotion.
     *
     * @param skuId    the identifier of the sku
     * @param quantity the quantity of sku
     * @return amount of exemption, the result will be 0 if the sku does not belong to this promotion
     */
    public BigDecimal simulateExemption(String skuId, int quantity) {
        if (!skuIds.contains(skuId)) {
            return BigDecimal.ZERO;
        }

        int times = (quantity + quantityPurchased) / promoMilestone;
        return discount.multiply(new BigDecimal(times));
    }

    /**
     * Given the sku id and the quantity of the sku, execute the calculation of amount of exemption and return it.
     * This method changes the state of the promotion itself.
     *
     * @param skuId    the identifier of the sku
     * @param quantity the quantity of sku
     * @return amount of exemption, the result will be 0 if the sku does not belong to this promotion
     */
    public BigDecimal executeExemption(String skuId, int quantity) {
        if (!skuIds.contains(skuId)) {
            return BigDecimal.ZERO;
        }

        quantityPurchased += quantity;
        int times = quantityPurchased / promoMilestone;
        quantityPurchased = quantityPurchased % promoMilestone;
        return discount.multiply(new BigDecimal(times));
    }

}
