package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.entity.LogisticExpense.AbstractLogisticExpenseDetail;
import org.jeecg.modules.business.entity.LogisticExpense.AnTuExpenseDetail;
import org.jeecg.modules.business.entity.LogisticExpenseDetail;
import org.jeecg.modules.business.vo.*;
import org.jeecg.modules.business.vo.dashboard.PeriodLogisticProfit;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Date;
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
     * @param country full country name
     * @param channelName chinese channel name
     * @return logistic profit of the month
     */
    PeriodLogisticProfit calculateLogisticProfitOf(Date startDate, Date endDate, List<String> country, List<String> channelName);

    List<LogisticExpenseProportion> calculateLogisticExpenseProportionByChannel(Date startDate, Date endDate, List<String> country, List<String> channelName);

    List<LogisticExpenseProportion> calculateLogisticExpenseProportionByCountry(Date startDate, Date endDate, List<String> country, List<String> channelName);

    Response<List<AbstractLogisticExpenseDetail>, String> antuExcelToObject(MultipartFile file);

    Response<List<AbstractLogisticExpenseDetail>, String> CNEExcelToObject(MultipartFile file);

    List<LogisticExpenseDetail> antuToLogisticExpenseDetail(List<AnTuExpenseDetail> antuExpenseDetails);

    List<String> allCountries();

    List<String> allChannels();

    boolean saveBatch(Collection<LogisticExpenseDetail> expenseDetails);

    Response<?, String> importExcel(MultipartFile file, LogisticCompanyEnum logisticCompanyEnum);
}
