package org.jeecg.modules.business.domain;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.*;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TemporaryJob implements Job {

    @Autowired
    @Setter
    private IPlatformOrderMabangService platformOrderMabangService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<Order> res = all28DaysOrdersOfStatus(OrderStatus.AllNonUnshipped);
        platformOrderMabangService.saveOrderFromMabang(res);

        List<Order> res2 = all28DaysOrdersOfStatus(OrderStatus.Completed);
        platformOrderMabangService.saveOrderFromMabang(res2);

        List<Order> res3 = all28DaysOrdersOfStatus(OrderStatus.Pending);
        platformOrderMabangService.saveOrderFromMabang(res3);
        log.info("Temporary job finished");
    }

    public List<Order> all28DaysOrdersOfStatus(OrderStatus status) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(1);
        List<Order> res = new ArrayList<>();
        try {
            // TODO change duration to 28
            for (int i = 0; i < 1; i++) {
                OrderListRequestBody body = OrderListRequestBodys.allOrderOfPaidDateOfStatus(start, end, status);
                OrderListRawStream rawStream = new OrderListRawStream(body);
                OrderListStream stream = new OrderListStream(rawStream);
                res.addAll(stream.all());

                end = end.minusDays(1);
                start = start.minusDays(1);
            }
            return res;
        } catch (OrderListRequestErrorException e) {
            throw new RuntimeException(e);
        }
    }

}
