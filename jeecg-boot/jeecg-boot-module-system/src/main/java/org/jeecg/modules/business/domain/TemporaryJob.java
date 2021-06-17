package org.jeecg.modules.business.domain;

import lombok.Setter;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.*;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TemporaryJob implements Job {

    @Autowired
    @Setter
    private IPlatformOrderMabangService platformOrderMabangService;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<Order> res = allOrder();
        platformOrderMabangService.saveOrderFromMabang(res);
    }

    public List<Order> allOrder() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(1);
        List<Order> res = new ArrayList<>();
        try {
            for (int i = 0; i < 28; i++) {

                OrderListRequestBody body = OrderListRequestBodys.allShippedOrderOfDate(start, end);
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
