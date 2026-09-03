package org.jeecg.modules.business.domain.job;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.mabang.orderDoDeliverOrder.OrderDeliverRequest;
import org.jeecg.modules.business.domain.api.mabang.orderDoDeliverOrder.OrderDeliverRequestBody;
import org.jeecg.modules.business.domain.api.mabang.orderDoDeliverOrder.OrderDeliverResponse;
import org.jeecg.modules.business.entity.ShoumanOrderWeight;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class ShoumanOrderDeliverToMabangJob implements Job {

    @Autowired
    private IPlatformOrderService platformOrderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Started Shouman order deliver-to-Mabang job");
        List<ShoumanOrderWeight> shoumanOrders = platformOrderService.findShoumanShippedButMabangUnshippedOrders();
        log.info("Retrieved {} orders shipped by Shouman but not yet shipped in Mabang", shoumanOrders.size());

        for (ShoumanOrderWeight shoumanOrderWeight : shoumanOrders) {
            String platformOrderId = shoumanOrderWeight.getPlatformOrderId();
            log.info("Started marking order {} as shipped in Mabang", platformOrderId);
            try {
                OrderDeliverRequest request = new OrderDeliverRequest(new OrderDeliverRequestBody(platformOrderId, shoumanOrderWeight.getWeight()));
                OrderDeliverResponse response = request.send();
                if (response == null) {
                    log.warn("Mabang deliver-order returned null response for {}", platformOrderId);
                    continue;
                }
                if (!response.success()) {
                    log.warn("Mabang deliver-order failed for {}, message={}", platformOrderId, response.getMessage());
                    continue;
                }
                log.info("Finished marking order {} as shipped in Mabang, message={}", platformOrderId, response.getMessage());
            } catch (Exception e) {
                log.error("Failed to mark order {} as shipped in Mabang", platformOrderId, e);
            }
        }

        log.info("Finished Shouman order deliver-to-Mabang job");
    }
}
