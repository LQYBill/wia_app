package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.remote.RemoteFileSystem;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.io.File.createTempFile;

@Slf4j
public class RetrieveOrderListJob implements Job {

    @Autowired
    @Setter
    private IPlatformOrderMabangService platformOrderMabangService;

    private final static Duration EXECUTION_DURATION = Duration.ofMinutes(30);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            updateNewOrder();
        } catch (OrderListRequestErrorException | IOException e) {
            throw new RuntimeException(e);
        }

        try {
            updateMergedOrder();
        } catch (OrderListRequestErrorException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Retrieve newly paid order.
     * Duration is specified by {@code EXECUTION_DURATION}
     */
    public void updateNewOrder() throws OrderListRequestErrorException, IOException {
        // request time parameter period
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime begin = end.minus(EXECUTION_DURATION);

        // sent request for newly paid orders
        OrderListRequestBody body = new OrderListRequestBody();
        body.setDatetimeType(DateType.PAID)
                .setStartDate(begin)
                .setEndDate(end)
                .setStatus(OrderStatus.AllUnshipped);
        OrderListRequest request = new OrderListRequest(body);
        List<Order> newlyPaidOrders = request.getAll();
        log.info("newly paid order size: {}", newlyPaidOrders.size());

        uploadToRemote(newlyPaidOrders,new Date().toString());

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
        log.info("Size of merged order:{}", mergedOrders.size());
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

    private void uploadToRemote(List<Order> orders, String name) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(orders);
        Path out =Files.createTempFile("data", "json");
        FileWriter writer = new FileWriter(out.toFile());
        writer.write(json);
        writer.close();
        RemoteFileSystem fs = new RemoteFileSystem("test");
        fs.cp(name, out.toFile());
    }
}
