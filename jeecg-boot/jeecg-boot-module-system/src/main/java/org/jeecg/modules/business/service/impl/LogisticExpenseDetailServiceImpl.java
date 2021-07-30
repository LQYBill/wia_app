package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.istack.internal.NotNull;
import org.jeecg.modules.business.entity.LogisticExpenseDetail;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.mapper.FactureDetailMapper;
import org.jeecg.modules.business.mapper.LogisticExpenseDetailMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.ILogisticExpenseDetailService;
import org.jeecg.modules.business.vo.FactureDetail;
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

    private FactureDetailMapper factureDetailMapper;


    @Override
    public PeriodLogisticProfit calculateLogisticProfitOf(Date start, Date stop, String country, String channelName) {

        List<PlatformOrderLogisticExpenseDetail> allOrders = logisticExpenseDetailMapper.findBetween(start, stop);

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
                BigDecimal.valueOf(7.8)
        );
    }

    private Map<LocalDate, BigDecimal> calculateAmountDueByDate(List<PlatformOrderLogisticExpenseDetail> invoicedOrders) {
        Set<String> invoiceCodes = invoicedOrders.stream().map(PlatformOrderLogisticExpenseDetail::getShippingInvoiceNumber).collect(Collectors.toSet());

        QueryWrapper<FactureDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("`N° de facture`", invoiceCodes);
        List<FactureDetail> factureDetails = factureDetailMapper.selectList(queryWrapper);
        Map<String, FactureDetail> invoiceCodeToFactureDetail = factureDetails.stream()
                .collect(Collectors.toMap(
                        FactureDetail::getFactureNum,
                        Function.identity()
                        )
                );

        Map<LocalDate, List<PlatformOrderLogisticExpenseDetail>> ordersByDate = invoicedOrders.stream().collect(Collectors.groupingBy(
                e -> e.getOrderTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                )
        );

        Map<LocalDate, BigDecimal> res = new HashMap<>();

        ordersByDate.forEach(
                (date, orders) -> {
                    BigDecimal amount = orders.stream()
                            .map(
                                    o -> {
                                        return invoiceCodeToFactureDetail.get(o.getShippingInvoiceNumber()).total();
                                    }
                            ).reduce(BigDecimal.ZERO, BigDecimal::add);
                    res.put(date, amount);
                });
        return res;
    }

    private Map<LocalDate, BigDecimal> calculateActualCostByDay(List<PlatformOrderLogisticExpenseDetail> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }

        // group by day of month
        Map<LocalDate, List<PlatformOrderLogisticExpenseDetail>> dateToOrders = orders.stream()
                .collect(
                        Collectors.groupingBy(
                                order -> order.getOrderTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
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
    public List<LogisticExpenseProportion> calculateLogisticExpenseProportionByChannel(Date start, Date stop) {
        return expenseBy(start, stop, PlatformOrderLogisticExpenseDetail::getLogisticChannelName);
    }

    @Override
    public List<LogisticExpenseProportion> calculateLogisticExpenseProportionByCountry(Date start, Date stop) {
        return expenseBy(start, stop, PlatformOrderLogisticExpenseDetail::getCountry);
    }

    private List<LogisticExpenseProportion> expenseBy(
            Date start,
            Date stop,
            @NotNull Function<PlatformOrderLogisticExpenseDetail, String> classifier
    ) {
        // find all orders of this month
        List<PlatformOrderLogisticExpenseDetail> orders = logisticExpenseDetailMapper.findBetween(start, stop);
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
