package org.jeecg.modules.business.domain.job;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.mabangapi.dochangeorder.ChangeOrderBody;
import org.jeecg.modules.business.domain.mabangapi.dochangeorder.ChangeOrderRequest;
import org.jeecg.modules.business.domain.mabangapi.dochangeorder.MainParameter;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.*;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
public class ChangPhoneNumberJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // search orders that paid in last 12 hours and pending
        OrderListRequestBody body = new OrderListRequestBody()
                .setStatus(OrderStatus.Pending)
                .setDatetimeType(DateType.PAID);

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(12);

        body.setStartDate(start);
        body.setEndDate(end);

        OrderListRawStream rawStream = new OrderListRawStream(body);
        OrderListStream stream = new OrderListStream(rawStream);
        List<Order> allOrders = stream.all();

        // find those don't have phone1
        Predicate<Order> noPhone = (order) -> order.getPhone1().isEmpty();
        // update 0954727318 as phone1
        allOrders.stream().filter(noPhone).forEach(
                order -> {
                    ChangeOrderBody changeOrderBody = new ChangeOrderBody(order.getPlatformOrderId());
                    changeOrderBody.setParameter(MainParameter.phone1, "0954727318");
                    ChangeOrderRequest request = new ChangeOrderRequest(changeOrderBody);
                    request.send();
                }
        );

    }


}
