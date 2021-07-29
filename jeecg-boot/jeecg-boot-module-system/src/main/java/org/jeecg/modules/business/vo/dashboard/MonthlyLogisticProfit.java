package org.jeecg.modules.business.vo.dashboard;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MonthlyLogisticProfit {

    @JSONField(name = "invoicedOrderNumber")
    private final int invoicedOrderNumber;

    @JSONField(name = "uninvoicedOrderNumber")
    private final int uninvoicedOrderNumber;

    @JSONField(name = "monthOfYear")
    private final int monthOfYear;

    /**
     * Index of elements is the date, begin from 0 to end of the month.
     */
    @JSONField(name = "invoicedAmountDue")
    private final List<BigDecimal> invoicedAmountDue;

    /**
     * Index of elements is the date, begin from 0 to end of the month.
     */
    @JSONField(name = "invoicedActualCosts")
    private final List<BigDecimal> invoicedActualCosts;

    /**
     * Index of elements is the date, begin from 0 to end of the month.
     */
    @JSONField(name = "nonInvoicedActualCosts")
    private final List<BigDecimal> nonInvoicedActualCosts;

    @JSONField(name = "exchangeRate")
    private final BigDecimal exchangeRate;
}
