package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.istack.NotNull;
import org.jeecg.modules.business.entity.LogisticExpenseDetail;
import org.jeecg.modules.business.mapper.LogisticExpenseDetailMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.ILogisticExpenseDetailService;
import org.jeecg.modules.business.vo.LogisticExpenseProportion;
import org.jeecg.modules.business.vo.PlatformOrderLogisticExpenseDetail;
import org.jeecg.modules.business.vo.dashboard.PeriodLogisticProfit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Description: 物流开销明细
 * @Author: jeecg-boot
 * @Date: 2021-07-22
 * @Version: V1.0
 */
@Service
public class LogisticExpenseDetailServiceImpl extends ServiceImpl<LogisticExpenseDetailMapper, LogisticExpenseDetail> implements ILogisticExpenseDetailService {

    @Autowired
    private PlatformOrderMapper platformOrderMapper;

    @Autowired
    private LogisticExpenseDetailMapper logisticExpenseDetailMapper;

    @Override
    public PeriodLogisticProfit calculateLogisticProfitOf(Date startDate, Date endDate, List<String> country, List<String> channelName) {

        List<PlatformOrderLogisticExpenseDetail> allOrders = logisticExpenseDetailMapper.findBetween(startDate, endDate, country, channelName);

        Predicate<PlatformOrderLogisticExpenseDetail> nonInvoiced = order -> order.getShippingInvoiceNumber() == null;
        Predicate<PlatformOrderLogisticExpenseDetail> invoiced = nonInvoiced.negate();

        // actual cost of invoiced orders
        List<PlatformOrderLogisticExpenseDetail> invoicedOrders = allOrders.stream().filter(invoiced).collect(Collectors.toList());
        Map<LocalDate, BigDecimal> invoicedActualCost = calculateActualCostByDay(invoicedOrders);
        // amount due of invoice
        Map<LocalDate, BigDecimal> amountDue = calculateAmountDueByDate(invoicedOrders);


        // actual cost of uninvoiced orders
        List<PlatformOrderLogisticExpenseDetail> nonInvoicedOrders = allOrders.stream().filter(nonInvoiced).collect(Collectors.toList());
        Map<LocalDate, BigDecimal> nonInvoicedActualCost = calculateActualCostByDay(nonInvoicedOrders);


        return new PeriodLogisticProfit(
                invoicedOrders.size(),
                nonInvoicedOrders.size(),
                amountDue,
                invoicedActualCost,
                nonInvoicedActualCost,
                BigDecimal.valueOf(7.6)
        );
    }

    private Map<LocalDate, BigDecimal> calculateAmountDueByDate(List<PlatformOrderLogisticExpenseDetail> invoicedOrders) {
        if (invoicedOrders.isEmpty()) {
            return Collections.emptyMap();
        }

        // group by day of month
        Map<LocalDate, List<PlatformOrderLogisticExpenseDetail>> dateToOrders = invoicedOrders.stream()
                .collect(
                        Collectors.groupingBy(
                                order -> order.getShippingTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                );


        Map<LocalDate, BigDecimal> dateToActualCost = new HashMap<>();

        dateToOrders.forEach(
                (date, ordersByDate) -> {
                    BigDecimal totalDue = ordersByDate.stream()
                            .flatMap(d -> Stream.of(d.getFretFee(), d.getShippingFee(), d.getVatFee()))
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    dateToActualCost.put(date, totalDue);
                }
        );

        return dateToActualCost;
    }

    private Map<LocalDate, BigDecimal> calculateActualCostByDay(List<PlatformOrderLogisticExpenseDetail> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }

        // group by day of month
        Map<LocalDate, List<PlatformOrderLogisticExpenseDetail>> dateToOrders = orders.stream()
                .collect(
                        Collectors.groupingBy(
                                order -> order.getShippingTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                );


        Map<LocalDate, BigDecimal> dateToActualCost = new HashMap<>();

        dateToOrders.forEach(
                (date, ordersByDate) -> {
                    BigDecimal cost = ordersByDate.stream()
                            .map(PlatformOrderLogisticExpenseDetail::getTotal_fee)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    dateToActualCost.put(date, cost);
                }
        );

        return dateToActualCost;
    }

    @Override
    public List<LogisticExpenseProportion> calculateLogisticExpenseProportionByChannel(Date startDate, Date endDate, List<String> country, List<String> channelName) {
        return expenseBy(startDate, endDate, country, channelName, PlatformOrderLogisticExpenseDetail::getLogisticChannelName);
    }

    @Override
    public List<LogisticExpenseProportion> calculateLogisticExpenseProportionByCountry(Date startDate, Date endDate, List<String> country, List<String> channelName) {
        return expenseBy(startDate, endDate, country, channelName, PlatformOrderLogisticExpenseDetail::getCountry);
    }

    private List<LogisticExpenseProportion> expenseBy(
            Date startDate,
            Date endDate,
            List<String> country,
            List<String> channelName,
            @NotNull Function<PlatformOrderLogisticExpenseDetail, String> classifier
    ) {
        // find all orders of this month
        List<PlatformOrderLogisticExpenseDetail> orders = logisticExpenseDetailMapper.findBetween(startDate, endDate, country, channelName);
        // group them by the classifier
        Map<String, List<PlatformOrderLogisticExpenseDetail>> groupedOrdersExpenseDetail = orders.stream().collect(Collectors.groupingBy(classifier));

        // define the function that reduce each part to expense proportion
        Function<Map.Entry<String, List<PlatformOrderLogisticExpenseDetail>>, LogisticExpenseProportion> ordersToExpense = (entry) -> {

            String name = entry.getKey();
            List<PlatformOrderLogisticExpenseDetail> expenseDetails = entry.getValue();

            BigDecimal expense = expenseDetails
                    .stream()
                    .map(PlatformOrderLogisticExpenseDetail::getTotal_fee)
                    .filter(Objects::nonNull)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );

            return new LogisticExpenseProportion(
                    null,
                    name,
                    expense);
        };

        return groupedOrdersExpenseDetail.entrySet().stream().map(ordersToExpense).collect(Collectors.toList());
    }

    @Override
    public List<String> allCountries() {
        return platformOrderMapper.allCountries();
    }

    @Override
    public List<String> allChannels() {
        return platformOrderMapper.allChannels();
    }
}
