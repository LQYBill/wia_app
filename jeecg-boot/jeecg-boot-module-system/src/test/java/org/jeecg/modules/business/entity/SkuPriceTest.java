package org.jeecg.modules.business.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


import java.math.BigDecimal;

public class SkuPriceTest {
    private final static SkuPrice normal_price = new SkuPrice();

    private final static SkuPrice nullDiscountPrice = new SkuPrice();

    private final static BigDecimal _4 = new BigDecimal(4);
    private final static BigDecimal _5 = new BigDecimal(5);


    @BeforeEach
    void initial() {
        normal_price.setPrice(_5);
        normal_price.setThreshold(20);
        normal_price.setDiscountedPrice(_4);

        nullDiscountPrice.setPrice(_5);
        nullDiscountPrice.setThreshold(20);
    }

    @Test
    void getPrice_Is_Correct() {
        assertEquals(_4, normal_price.getPrice(20));
        assertEquals(_5, normal_price.getPrice(10));

        assertEquals(_5, nullDiscountPrice.getPrice(20));
        assertEquals(_5, nullDiscountPrice.getPrice(10));
    }
}
