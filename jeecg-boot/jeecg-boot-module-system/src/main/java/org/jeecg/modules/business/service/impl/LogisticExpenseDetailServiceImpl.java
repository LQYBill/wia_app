package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.istack.internal.NotNull;
import org.jeecg.modules.business.entity.LogisticExpenseDetail;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.mapper.LogisticExpenseDetailMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.ILogisticExpenseDetailService;
import org.jeecg.modules.business.vo.LogisticExpenseProportion;
import org.jeecg.modules.business.vo.dashboard.MonthlyLogisticProfit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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


    @Override
    public MonthlyLogisticProfit calculateMonthlyLogisticProfit(Month month, String country, String channelName) {

        List<PlatformOrder> orderOfMonth = platformOrderMapper.findByMonthCountryChannel(month.getValue(), country, channelName);

        Predicate<PlatformOrder> nonInvoiced = order -> order.getShippingInvoiceNumber() == null;
        Predicate<PlatformOrder> invoiced = nonInvoiced.negate();

        // actual cost of invoiced orders
        List<PlatformOrder> invoicedOrders = orderOfMonth.stream().filter(invoiced).collect(Collectors.toList());
        List<BigDecimal> invoicedActualCost = Arrays.asList(calculateActualCostByDay(invoicedOrders));
        // amount due of invoice
        Map<Integer, List<PlatformOrder>> ordersByDay = invoicedOrders.stream()
                .collect(
                        Collectors.groupingBy(
                                order -> order.getOrderTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth()
                        )
                );
        // result length is today's date
        int length = LocalDate.now().getDayOfMonth();
        BigDecimal[] res = new BigDecimal[length];

        // for each day from begin of the month
        for (int i = 0; i < res.length; i++) {
            List<PlatformOrder> ordersOfDay = ordersByDay.get(i);
            // if no orders in that day, actual cost is zero.
            if (ordersOfDay == null) {
                res[i] = BigDecimal.ZERO;
            } else {
                // otherwise, it's sum of all orders of that day.
                List<String> codes = ordersOfDay.stream()
                        .map(PlatformOrder::getShippingInvoiceNumber)
                        .collect(Collectors.toList());
                res[i] = logisticExpenseDetailMapper.findExpenseByInvoiceCodes(codes)
                        .stream()
                        .map(e -> e == null ? BigDecimal.ZERO : e)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        List<BigDecimal> amountDue = Arrays.asList(res);


        // actual cost of uninvoiced orders
        List<PlatformOrder> nonInvoicedOrders = orderOfMonth.stream().filter(nonInvoiced).collect(Collectors.toList());
        List<BigDecimal> nonInvoicedActualCost = Arrays.asList(calculateActualCostByDay(nonInvoicedOrders));


        return new MonthlyLogisticProfit(
                invoicedOrders.size(),
                nonInvoicedOrders.size(),
                month.getValue(),
                amountDue,
                invoicedActualCost,
                nonInvoicedActualCost,
                BigDecimal.valueOf(7.8)
        );
    }

    private BigDecimal[] calculateActualCostByDay(List<PlatformOrder> orders) {
        if (orders.isEmpty()) {
            int length = LocalDate.now().getDayOfMonth();
            BigDecimal[] res = new BigDecimal[length];
            // for each day from begin of the month
            Arrays.fill(res, BigDecimal.ZERO);
            return res;
        }
        List<String> ids = orders.stream().map(PlatformOrder::getPlatformOrderId).collect(Collectors.toList());

        Map<String, LogisticExpenseDetail> serialIdToExpense = logisticExpenseDetailMapper.findBy(ids).stream()
                .collect(Collectors.toMap(LogisticExpenseDetail::getPlatformOrderSerialId, Function.identity()));

        // group by day of month
        Map<Integer, List<PlatformOrder>> dateToOrder = orders.stream()
                .collect(
                        Collectors.groupingBy(
                                order -> order.getOrderTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth()
                        )
                );

        // result length is today's date
        int length = LocalDate.now().getDayOfMonth();
        BigDecimal[] res = new BigDecimal[length];

        // for each day from begin of the month
        for (int i = 0; i < res.length; i++) {
            List<PlatformOrder> ordersOfDay = dateToOrder.get(i);
            // if no orders in that day, actual cost is zero.
            if (ordersOfDay == null) {
                res[i] = BigDecimal.ZERO;
            } else {
                // otherwise, it's sum of all orders of that day.
                res[i] = ordersOfDay.stream()
                        .map(PlatformOrder::getPlatformOrderId)
                        .map(serialIdToExpense::get)
                        .filter(Objects::nonNull)
                        .map(LogisticExpenseDetail::getTotalFee)
                        .filter(number -> !number.equals(BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }

        return res;
    }

    @Override
    public List<LogisticExpenseProportion> calculateLogisticExpenseProportionByChannel() {
        return expenseBy(PlatformOrder::getLogisticChannelName);
    }

    @Override
    public List<LogisticExpenseProportion> calculateLogisticExpenseProportionByCountry() {
        return expenseBy(PlatformOrder::getCountry);
    }

    private List<LogisticExpenseProportion> expenseBy(@NotNull Function<PlatformOrder, String> classifier) {
        List<PlatformOrder> orders = platformOrderMapper.findByMonthCountryChannel(LocalDate.now().getMonth().getValue(), null, null);
        Map<String, List<PlatformOrder>> ordersByCountry = orders.stream().collect(Collectors.groupingBy(classifier));

        Function<Map.Entry<String, List<PlatformOrder>>, LogisticExpenseProportion> ordersToExpense = (entry) -> {
            List<String> serial_ids = entry.getValue().stream().map(PlatformOrder::getPlatformOrderId).collect(Collectors.toList());
            BigDecimal expense = logisticExpenseDetailMapper
                    .findBy(serial_ids)
                    .stream()
                    .filter(Objects::nonNull)
                    .map(LogisticExpenseDetail::getTotalFee)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add

                    );
            return new LogisticExpenseProportion(
                    null,
                    entry.getKey(),
                    expense);
        };

        return ordersByCountry.entrySet().stream().map(ordersToExpense).collect(Collectors.toList());
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
