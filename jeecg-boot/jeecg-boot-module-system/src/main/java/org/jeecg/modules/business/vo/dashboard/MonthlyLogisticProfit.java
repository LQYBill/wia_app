package org.jeecg.modules.business.vo.dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MonthlyLogisticProfit {

    private final int monthOfYear;

    private final List<DailyLogisticProfit> dailyData;

    private final BigDecimal exchangeRate;
}
