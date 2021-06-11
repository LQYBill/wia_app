package org.jeecg.modules.business.vo;

import org.assertj.core.util.Lists;
import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.entity.SkuPrice;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;
import org.junit.jupiter.params.ParameterizedTest;

import static org.junit.jupiter.params.provider.Arguments.*;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

public class OrdersStatisticDataTest {

    @ParameterizedTest
    @MethodSource("orderContentDetailProvider")
    void makeDataCorrect_Is_Correct(List<OrderContentDetail> source, BigDecimal totalPrice, BigDecimal reducedAmount, int skuNumber, int quantity) {
        OrdersStatisticData data = OrdersStatisticData.makeData(source, null);
        assertEquals(totalPrice, data.getEstimatedTotalPrice());
        assertEquals(reducedAmount, data.getReducedAmount());
        assertEquals(skuNumber, data.getSkuNumber());
        assertEquals(quantity, data.getTotalQuantity());
    }

    static Stream<Arguments> orderContentDetailProvider() {
        OrderContentDetail o1 = mock(OrderContentDetail.class);
        when(o1.totalPrice()).thenReturn(decimal(20));
        when(o1.reducedAmount()).thenReturn(decimal(50));

        OrderContentDetail o2 = mock(OrderContentDetail.class);
        when(o1.totalPrice()).thenReturn(decimal(10));
        when(o1.reducedAmount()).thenReturn(decimal(5));

        OrderContentDetail o3 = mock(OrderContentDetail.class);
        when(o1.totalPrice()).thenReturn(decimal(100));
        when(o1.reducedAmount()).thenReturn(decimal(20));

        OrderContentDetail o4 = mock(OrderContentDetail.class);
        when(o1.totalPrice()).thenReturn(decimal(0));
        when(o1.reducedAmount()).thenReturn(decimal(0));

        return Stream.of(
                arguments(
                        Lists.list(o1, o2),
                        decimal(150),
                        decimal(150),
                        1,
                        1
                )
        );
    }

    static SkuPrice aPrice(BigDecimal price, int threshold, BigDecimal discountedPrice) {
        SkuPrice skuPrice = new SkuPrice();
        skuPrice.setPrice(price);
        skuPrice.setThreshold(threshold);
        skuPrice.setDiscountedPrice(discountedPrice);
        return skuPrice;
    }

    private static BigDecimal decimal(int n) {
        return new BigDecimal(n);
    }

    private static BigDecimal decimal(double n) {
        return new BigDecimal(n);
    }

}
