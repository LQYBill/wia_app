package org.jeecg.modules.business.vo.dashboard;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailyLogisticProfit {

    private final int invoicedOrderNumber;
    private final int uninvoicedOrderNumber;

    private final BigDecimal amountReceivable;
    private final BigDecimal actualCost;

    private final BigDecimal uninvoicedAmountReceivable;
    private final BigDecimal uninvoicedActualCost;
}
