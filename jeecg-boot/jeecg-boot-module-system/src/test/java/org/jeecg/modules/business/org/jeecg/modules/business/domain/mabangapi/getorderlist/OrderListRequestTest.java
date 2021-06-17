package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi.getorderlist;

import org.jeecg.modules.business.domain.mabangapi.getorderlist.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class OrderListRequestTest {

    /**
     * A test that retrieves orders paid in a specific time period,
     * not working between 7:00 and 19:00 GMT+8.
     */
    @Test
    void testToReq1() throws OrderListRequestErrorException {
        OrderListRequestBody body = new OrderListRequestBody();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(400);
        body.setStatus(OrderStatus.Pending)
                .setDatetimeType(DateType.PAID)
                .setStartDate(start)
                .setEndDate(end);

        OrderListRequest request = new OrderListRequest(body);
        int count = 0;
        List<Order> orders = new ArrayList<>();
        while (request.hasNext()) {
            orders.add(request.next());
        }

        System.out.println(orders.size());

    }

    /**
     * A test that retrieves orders that paid in last 2 hours.
     */
    @Test
    void testToReq2() throws OrderListRequestErrorException {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(2);

        OrderListRequestBody body = new OrderListRequestBody();
        body.setStatus(OrderStatus.Pending)
                .setDatetimeType(DateType.PAID)
                .setStartDate(start)
                .setEndDate(end);
        OrderListRequest request = new OrderListRequest(body);
        List<Order> paidOrders = new ArrayList<>();
        while (request.hasNext()) {
            paidOrders.add(request.next());
        }

        OrderListRequestBody bodyForUpdate = new OrderListRequestBody();
        bodyForUpdate.setStatus(OrderStatus.Pending)
                .setDatetimeType(DateType.UPDATE)
                .setStartDate(start)
                .setEndDate(end);
        OrderListRequest requestForUpdate = new OrderListRequest(bodyForUpdate);
        List<Order> updateOrders = new ArrayList<>();
        while (requestForUpdate.hasNext()) {
            updateOrders.add(requestForUpdate.next());
        }
        System.out.println("paid size: " + paidOrders.size());
        System.out.println("update size: " + updateOrders.size());
    }

    @Test
    void updateInfo() throws OrderListRequestErrorException {
        LocalDateTime end = LocalDateTime.of(2021, Month.JUNE, 14, 15, 0);
        LocalDateTime start = end.minusHours(24);

        OrderListRequestBody bodyForUpdate = new OrderListRequestBody();
        bodyForUpdate.setStatus(OrderStatus.AllUnshipped)
                .setDatetimeType(DateType.UPDATE)
                .setStartDate(start)
                .setEndDate(end);
        OrderListRequest requestForUpdate = new OrderListRequest(bodyForUpdate);
        List<Order> updateOrders = new ArrayList<>();
        while (requestForUpdate.hasNext()) {
            updateOrders.add(requestForUpdate.next());
        }
        List<Order> unionOrder = updateOrders.stream().filter(Order::isUnion).collect(Collectors.toList());


        System.out.printf("Size of union order %d\n", unionOrder.size());
        for (Order o : unionOrder) {
            System.out.println(o);
        }

        System.out.printf("Size of updated order %d\n", updateOrders.size());


    }

    @Test
    void ObsoletedOrder() throws OrderListRequestErrorException {
        LocalDateTime end = LocalDateTime.of(2021, Month.JUNE, 13, 17, 0);
        LocalDateTime start = end.minusHours(24);

        OrderListRequestBody bodyForUpdate = new OrderListRequestBody();
        bodyForUpdate.setStatus(OrderStatus.Pending)
                .setDatetimeType(DateType.UPDATE)
                .setStartDate(start)
                .setEndDate(end);
        OrderListRequest requestForUpdate = new OrderListRequest(bodyForUpdate);
        List<Order> unionOrder = requestForUpdate.getAll().stream().filter(Order::isUnion).collect(Collectors.toList());


        OrderListRequestBody bodyOfOrder = new OrderListRequestBody();
        bodyOfOrder.setStatus(OrderStatus.Obsolete)
                .setDatetimeType(DateType.UPDATE)
                .setStartDate(start)
                .setEndDate(end);
        OrderListRequest orderListRequest = new OrderListRequest(bodyOfOrder);
        List<Order> obsoletedOrders = orderListRequest.getAll();

        for(Order o:obsoletedOrders ){
            System.out.printf("%s %s\n", o.getPlatformOrderId(), o.getStatus());
        }
        System.out.println("Obsoleted size:"+obsoletedOrders.size());
        for(Order o:unionOrder ){
            System.out.println(o);
        }
        System.out.println("union size:"+unionOrder.size());



    }
}
