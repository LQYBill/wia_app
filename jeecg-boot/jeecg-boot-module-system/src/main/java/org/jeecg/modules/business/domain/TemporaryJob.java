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
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(1);
        try {
            for (int i = 0; i < 28; i++) {
                OrderListRequestBody body = OrderListRequestBodys.allOrderOfPaidDateOfStatus(start, end, OrderStatus.AllUnshipped);
                OrderListRequest request = new OrderListRequest(body);
                List<Order> unshipped = request.getAll();
                log.trace("{} unshipped orders from {} to {} to be inserted/update.", start, end, unshipped.size());
                platformOrderMabangService.saveOrderFromMabang(unshipped);

                body = OrderListRequestBodys.allOrderOfPaidDateOfStatus(start, end, OrderStatus.Completed);
                request = new OrderListRequest(body);
                List<Order> completed = request.getAll();
                log.trace("{} completed orders from {} to {} to be inserted/update.", start, end, completed.size());
                platformOrderMabangService.saveOrderFromMabang(completed);

                body = OrderListRequestBodys.allOrderOfPaidDateOfStatus(start, end, OrderStatus.Shipped);
                request = new OrderListRequest(body);
                List<Order> shipped = request.getAll();
                log.trace("{} shipped orders from {} to {} to be inserted/update.", start, end, shipped.size());
                platformOrderMabangService.saveOrderFromMabang(shipped);

                end = end.minusDays(1);
                start = start.minusDays(1);
            }
        } catch (OrderListRequestErrorException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Order> all28DaysOrdersOfStatus(OrderStatus status) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(1);
        List<Order> res = new ArrayList<>();
        try {
            for (int i = 0; i < 1; i++) {
                OrderListRequestBody body = OrderListRequestBodys.allOrderOfPaidDateOfStatus(start, end, status);
                OrderListRequest request = new OrderListRequest(body);
                res.addAll(request.getAll());

                end = end.minusDays(1);
                start = start.minusDays(1);
            }
            return res;
        } catch (OrderListRequestErrorException e) {
            throw new RuntimeException(e);
        }
    }

}
