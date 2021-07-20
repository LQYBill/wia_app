package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import org.jeecg.modules.business.domain.mabangapi.dochangeorder.ChangeOrderBody;
import org.jeecg.modules.business.domain.mabangapi.dochangeorder.ChangeOrderRequest;
import org.jeecg.modules.business.domain.mabangapi.dochangeorder.MainParameter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class streamTest {

    @Test
    void stream_Test() {
        OrderListRequestBody body = new OrderListRequestBody()
                .setStatus(OrderStatus.Pending)
                .setDatetimeType(DateType.PAID);

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(12);

        body.setStartDate(start);
        body.setEndDate(end);

        OrderListRawStream rawStream = new OrderListRawStream(body);
        OrderListStream stream = new OrderListStream(rawStream);

        List<Order> orders = stream.all();
        List<Order> nophoneOrders = orders.stream().filter((order) ->
                order.getPhone1().isEmpty()
        ).collect(Collectors.toList());

        List<String> ids = orders.stream().map(Order::getPlatformOrderId).collect(Collectors.toList());

        System.out.println(ids);
        System.out.println(ids.contains("3903592825004"));

        System.out.println("Total orders: " + orders.size());
        System.out.println("Orders without phone");
        System.out.println(nophoneOrders);

        System.out.println(orders.stream().filter((order) -> order.getPlatformOrderId().equals("3903592825004")).collect(Collectors.toList()));

        ChangeOrderBody body1 = new ChangeOrderBody("3903592825004");

        body1.setParameter(MainParameter.phone1, "0954727318");

        ChangeOrderRequest request = new ChangeOrderRequest(body1);
        System.out.println(request.send());

    }
}
