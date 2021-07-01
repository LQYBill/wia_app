package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi.getorderlist;

import org.jeecg.modules.business.domain.mabangapi.getorderlist.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

public class OrderListStreamTest {


    @Test
    void testNext() {
        OrderListRequestBody body = new OrderListRequestBody();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(3);
        body.setDatetimeType(DateType.UPDATE)
                .setStartDate(start)
                .setEndDate(end);

        OrderListRawStream rawStream = new OrderListRawStream(body);
        OrderListStream stream = new OrderListStream(rawStream);
        int count = 0;
        while (stream.hasNext()) {
            stream.next();
            count++;
        }
        System.out.println(count);
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
