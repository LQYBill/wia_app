package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import java.time.LocalDateTime;

public class OrderListRequestBodys {

    public static OrderListRequestBody allShippedOrderOfDate(LocalDateTime start, LocalDateTime end){
        return allOrderOfPaidDateOfStatus(start,end,OrderStatus.AllNonUnshipped);
    }


    public static OrderListRequestBody allPendingOrderOfDate(LocalDateTime start, LocalDateTime end){
        return allOrderOfPaidDateOfStatus(start,end,OrderStatus.Pending);

    }

    public static OrderListRequestBody allFinishedOrderOfDate(LocalDateTime start, LocalDateTime end){
        return allOrderOfPaidDateOfStatus(start,end,OrderStatus.Completed);

    }

    public static OrderListRequestBody allOrderOfPaidDateOfStatus(LocalDateTime start, LocalDateTime end, OrderStatus status){
        return new OrderListRequestBody()
                .setStatus(status)
                .setDatetimeType(DateType.PAID)
                .setStartDate(start)
                .setEndDate(end);
    }
}
