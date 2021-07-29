package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.entity.LogisticExpenseDetail;
import org.jeecg.modules.business.vo.LogisticExpenseProportion;
import org.jeecg.modules.business.vo.dashboard.MonthlyLogisticProfit;

import java.time.Month;
import java.util.List;

/**
 * @Description: 物流开销明细
 * @Author: jeecg-boot
 * @Date: 2021-07-22
 * @Version: V1.0
 */
public interface ILogisticExpenseDetailService extends IService<LogisticExpenseDetail> {

    /**
     * Calculate monthly profit of a month.
     *
     * @param month the month, begin from 1 to 12.
     * @param country full country name
     * @param channelName chinese channel name
     * @return logistic profit of the month
     */
    MonthlyLogisticProfit calculateMonthlyLogisticProfit(Month month, String country, String channelName);

    List<LogisticExpenseProportion> calculateLogisticExpenseProportionByChannel();

    List<LogisticExpenseProportion> calculateLogisticExpenseProportionByCountry();

    List<String> allCountries();

    List<String> allChannels();
}
