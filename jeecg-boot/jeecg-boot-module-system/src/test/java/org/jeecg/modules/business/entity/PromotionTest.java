package org.jeecg.modules.business.entity;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class PromotionTest {


    @Test
    void Test_Calculate_One_Discount_Is_Right() {
        BigDecimal expectedNumber = new BigDecimal(80);

        Promotion promotion = new Promotion("2123", 100, 1, new BigDecimal(80));
        assertEquals(promotion.calculateDiscount(99), expectedNumber);

        Promotion promotion2 = new Promotion("2123", 100, 50, new BigDecimal(80));
        assertEquals(promotion2.calculateDiscount(99), expectedNumber);
    }

    @Test
    void Test_Calculate_Multiple_Discount_Is_Right() {
        BigDecimal expectedNumber = new BigDecimal(160);

        Promotion promotion = new Promotion("2123", 100, 0, new BigDecimal(80));
        assertEquals(promotion.calculateDiscount(200), expectedNumber);

        Promotion promotion2 = new Promotion("2123", 100, 50, new BigDecimal(80));
        assertEquals(promotion2.calculateDiscount(200), expectedNumber);
    }

    @Test
    void Test_Calculate_Zero_Discount_Is_Right() {
        BigDecimal expectedNumber = new BigDecimal(0);

        Promotion promotion = new Promotion("2123", 100, 0, new BigDecimal(80));
        assertEquals(promotion.calculateDiscount(20), expectedNumber);

        Promotion promotion2 = new Promotion("2123", 100, 50, new BigDecimal(80));
        assertEquals(promotion2.calculateDiscount(49), expectedNumber);
    }

    @Test
    void Test_addPurchasedQuantity_Is_Right() {
        Promotion p = new Promotion("2123", 100, 0, new BigDecimal(80));
        assertEquals(50, p.addPurchaseQuantity(50).getQuantityPurchased());

        assertEquals(50, p.addPurchaseQuantity(150).getQuantityPurchased());

        assertEquals(0, p.addPurchaseQuantity(0).getQuantityPurchased());

    }
}
