package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi;

import org.jeecg.modules.business.domain.mabangapi.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;


public class OrderListRequestTest {

    @Test
    void testToReq() throws OrderListRequestErrorException {
        OrderListRequestBody body = new OrderListRequestBody();
        body.setStatus(OrderStatus.Pending)
                .setDatetimeType(DateType.PAID)
                .setStartDate(LocalDateTime.of(2021, Month.JUNE, 1, 12, 30,59))
                .setEndDate(LocalDateTime.of(2021, Month.JUNE, 1, 14, 30,59));
        OrderListResponse res = OrderListRequest.sendRequest(body);
        // 137

        OrderListRequest request = new OrderListRequest(body);
        int count = 0;
        while (request.hasNext()) {
            count += request.next().size();
        }

        System.out.println(count);

    }
}
