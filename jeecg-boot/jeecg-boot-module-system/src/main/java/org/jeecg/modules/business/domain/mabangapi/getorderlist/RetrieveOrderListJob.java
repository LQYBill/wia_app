package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class RetrieveOrderListJob implements Job {

    @Autowired
    @Setter
    private IPlatformOrderMabangService platformOrderMabangService;

    private final static Duration EXECUTION_DURATION = Duration.ofHours(6);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 1. request orders that newly paid in last 30 minutes and save them to db.
        // 2. request orders that newly updated in last 30 minutes,
    }

    /**
     * Retrieve last 30 minutes newly paid order.
     * Duration is specified by {@code EXECUTION_DURATION}
     */
    public void updateNewOrder() throws OrderListRequestErrorException {
        // request time parameter period: now - 30m -> now
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime begin = end.minus(EXECUTION_DURATION);

        // sent request for newly paid orders
        OrderListRequestBody body = new OrderListRequestBody();
        body.setDatetimeType(DateType.PAID)
                .setStartDate(begin)
                .setEndDate(end);
        OrderListRequest request = new OrderListRequest(body);
        List<Order> newlyPaidOrders = request.getAll();
        // update in DB
        platformOrderMabangService.saveOrderFromMabang(newlyPaidOrders);
    }

    /**
     * Retrieve newly changed order and merge them.
     * Duration is specified by {@code EXECUTION_DURATION}
     */
    public void updateMergedOrder() throws OrderListRequestErrorException {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime begin = end.minus(EXECUTION_DURATION);

        // Query orders that updated in a certain duration of time in the past.
        OrderListRequestBody updatedOrderBody = new OrderListRequestBody();
        updatedOrderBody
                .setStartDate(begin)
                .setEndDate(end)
                .setDatetimeType(DateType.UPDATE)
                .setStatus(OrderStatus.Pending);
        OrderListRequest updatedOrderRequest = new OrderListRequest(updatedOrderBody);
        List<Order> updatedOrders = updatedOrderRequest.getAll();
        log.debug("Size of updated order:{}", updatedOrders.size());

        // filter orders that are merged
        List<Order> mergedOrders = updatedOrders.stream()
                .filter(Order::isUnion)
                .collect(Collectors.toList());
        log.debug("Size of merged order:{}", mergedOrders.size());
        // search other source orders of the merged order and make a map

        Map<Order, Set<String>> mergedOrderToSourceOrdersErpId = mergedOrders.stream()
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                o -> o.getOrderItems().stream()
                                        .filter(
                                                item -> !item.getOriginOrderId().equals(o.getErpOrderId())
                                        )
                                        .map(OrderItem::getOriginOrderId)
                                        .collect(Collectors.toSet())

                        )
                );

        // update in DB
        mergedOrderToSourceOrdersErpId.forEach(platformOrderMabangService::updateMergedOrderFromMabang);
    }


    /**
     * Search merge sources of target order from mabang API.
     *
     * @param target target order
     * @return source orders of the target order
     */
    private static List<Order> searchMergeSources(Order target) throws OrderListRequestErrorException {
        // search period: target order paid time - now
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime begin = end.minus(EXECUTION_DURATION);
        // send request
        OrderListRequestBody obsoletedOrderRequestBody = new OrderListRequestBody();
        obsoletedOrderRequestBody.setStatus(OrderStatus.Obsolete)
                .setDatetimeType(DateType.UPDATE)
                .setStartDate(begin)
                .setEndDate(end);
        OrderListRequest requestForObsoletedOrder = new OrderListRequest(obsoletedOrderRequestBody);
        // filter results and return them
        return requestForObsoletedOrder.getAll().stream()
                .filter(target::isSource)
                .collect(Collectors.toList());
    }
}
