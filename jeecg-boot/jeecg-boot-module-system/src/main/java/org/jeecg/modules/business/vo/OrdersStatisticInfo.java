package org.jeecg.modules.business.vo;


import lombok.Data;

import java.math.BigDecimal;

/**
 * This class is used to display details of multiple orders
 */
@Data
public class OrdersStatisticInfo {
    private final int totalQuantity;

    private final int skuNumber;

    private final BigDecimal estimatedPrice;
}
