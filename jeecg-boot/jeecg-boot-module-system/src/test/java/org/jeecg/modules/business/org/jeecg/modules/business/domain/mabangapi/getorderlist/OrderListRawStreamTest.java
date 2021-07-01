package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi.getorderlist;

import org.jeecg.modules.business.domain.mabangapi.getorderlist.DateType;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderListRawStream;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderListRequestBody;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderListResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

public class OrderListRawStreamTest {

    @Test
    void testNext() {
        OrderListRequestBody body = new OrderListRequestBody();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(3);
        body.setDatetimeType(DateType.UPDATE)
                .setStartDate(start)
                .setEndDate(end);

        OrderListRawStream rawStream = new OrderListRawStream(body);
        while (rawStream.hasNext()) {
            System.out.println(rawStream.next());
        }
    }

    @Test
    void test_all() {
        OrderListRequestBody body = new OrderListRequestBody();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(3);
        body.setDatetimeType(DateType.UPDATE)
                .setStartDate(start)
                .setEndDate(end);

        List<OrderListResponse> responseList = new OrderListRawStream(body).all();
        System.out.println(responseList);
    }
}
