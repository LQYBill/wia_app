package org.jeecg.modules.business.vo.clientPlatformOrder.section;


import lombok.Data;
import org.jeecg.modules.business.entity.OrderContentDetail;

import java.math.BigDecimal;
import java.util.List;

/**
 * This class is used to display details of multiple orders
 * in the head section of client platform order page
 */
@Data
public class OrdersStatisticData {
    private final int totalQuantity;

    private final int skuNumber;

    private final BigDecimal estimatedTotalPrice;

    private final BigDecimal reducedAmount;

    private OrdersStatisticData(int totalQuantity, int skuNumber, BigDecimal estimatedTotalPrice, BigDecimal reducedAmount) {
        this.totalQuantity = totalQuantity;
        this.skuNumber = skuNumber;
        this.estimatedTotalPrice = estimatedTotalPrice;
        this.reducedAmount = reducedAmount;
    }

    public static OrdersStatisticData makeData(List<OrderContentDetail> source) {
        int totalQuantity = source.stream().mapToInt(OrderContentDetail::getQuantity).sum();

        int skuNumber = source.size();

        BigDecimal estimatedTotalPrice = BigDecimal.ZERO;
        BigDecimal reducedAmount = BigDecimal.ZERO;

        for (OrderContentDetail d : source) {
            estimatedTotalPrice = estimatedTotalPrice.add(d.totalPrice());
            reducedAmount = reducedAmount.add(d.reducedAmount());
        }
        return new OrdersStatisticData(totalQuantity, skuNumber, estimatedTotalPrice, reducedAmount);
    }
}
