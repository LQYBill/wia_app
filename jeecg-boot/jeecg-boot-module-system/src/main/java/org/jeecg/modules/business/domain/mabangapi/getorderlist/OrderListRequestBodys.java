package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import java.time.LocalDateTime;

public class OrderListRequestBodys {

    public static OrderListRequestBody allShippedOrderOfDate(LocalDateTime start, LocalDateTime end){
        return new OrderListRequestBody()
                .setStatus(OrderStatus.AllNonUnshipped)
                .setDatetimeType(DateType.PAID)
                .setStartDate(start)
                .setEndDate(end);
    }
}
