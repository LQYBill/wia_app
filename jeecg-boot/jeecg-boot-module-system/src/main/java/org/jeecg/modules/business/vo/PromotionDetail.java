package org.jeecg.modules.business.vo;

/**
 * Data object that records the purchase that activates
 * the promotion and the number of activation.
 */
public class PromotionDetail {
    final String promotionID;

    final String skuID;

    final int count;

    public PromotionDetail(String promotionID, String skuID, int count) {
        this.promotionID = promotionID;
        this.skuID = skuID;
        this.count = count;
    }
}
