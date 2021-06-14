package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import org.jeecg.modules.business.service.impl.PlatformOrderServiceImpl;
import org.jeecg.modules.business.service.impl.purchase.PlatformOrderContentServiceImpl;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RetrieveOrderListJob implements Job {

    @Autowired
    private PlatformOrderServiceImpl platformOrderService;
    @Autowired
    private PlatformOrderContentServiceImpl platformOrderContentService;

    private final static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:dd", Locale.CHINA);


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 1. request orders that newly paid in last 30 minutes and save them to db.
        // 2. request orders that newly updated in last 30 minutes,
    }

    /**
     * Retrieve last 30 minutes newly paid order.
     */
    public void updateNewOrder() throws OrderListRequestErrorException {
        OrderListRequestBody body = new OrderListRequestBody();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime begin = end.minusMinutes(30);

        body.setDatetimeType(DateType.PAID)
                .setStartDate(begin)
                .setEndDate(end);

        OrderListRequest request = new OrderListRequest(body);
        List<Order> newlyPaidOrders = request.getAll();
        platformOrderService.saveOrderFromMabang(newlyPaidOrders);
    }

    /**
     * Retrieve changed order and merge them.
     */
    public void updateMergedOrder(Duration duration) throws OrderListRequestErrorException {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime begin = end.minus(duration);

        // Query orders that updated in a certain duration of time in the past.
        OrderListRequestBody updatedOrderBody = new OrderListRequestBody();
        updatedOrderBody
                .setStartDate(begin)
                .setEndDate(end)
                .setDatetimeType(DateType.UPDATE)
                .setStatus(OrderStatus.Pending);
        OrderListRequest updatedOrderRequest = new OrderListRequest(updatedOrderBody);
        List<Order> updatedOrders = updatedOrderRequest.getAll();
        // filter orders that are merged
        List<Order> mergedOrders = updatedOrders.stream()
                .filter(this::isMergedOrder)
                .collect(Collectors.toList());
        // search other source orders of the merged order and make a map
        Map<Order, List<Order>> mergedOrderToSourceOrders = mergedOrders.stream()
                .collect(Collectors.toMap(Function.identity(), this::searchMergeSources));
        // update in DB
        mergedOrderToSourceOrders.forEach(platformOrderService::updateOrderFromMabang);
    }


    private boolean isMergedOrder(Order order) {
        Map<String, Integer> erpCodeToQuantity = order.getOrderItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getErpCode,
                        OrderItem::getQuantity
                        )
                );
        return platformOrderContentService.hasMoreContent(order.getPlatformOrderNumber(), erpCodeToQuantity);
    }

    /**
     * Search merge sources of the order in argument.
     *
     * @param order
     * @return
     */
    private List<Order> searchMergeSources(Order order) throws OrderListRequestErrorException {
        LocalDateTime begin = LocalDateTime.ofInstant(order.getOrderTime().toInstant(), ZoneOffset.ofHours(8));
        LocalDateTime end = LocalDateTime.now();
        OrderListRequestBody obsoletedOrderRequestBody = new OrderListRequestBody();
        obsoletedOrderRequestBody.setStatus(OrderStatus.Obsolete)
                .setDatetimeType(DateType.UPDATE)
                .setStartDate(begin)
                .setEndDate(end);
        OrderListRequest requestForObsoletedOrder = new OrderListRequest(obsoletedOrderRequestBody);
        return requestForObsoletedOrder.getAll().stream()
                .filter(candidat -> isSource(order, candidat))
                .collect(Collectors.toList());
    }

    /**
     * Determine if candidate is the source of the target.
     *
     * @param target   target order
     * @param candidate candidat order
     * @return true if it is
     */
    private static boolean isSource(Order target, Order candidate) {
        return false;
    }
}
