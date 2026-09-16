package org.jeecg.modules.business.domain.job;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.CancelOrderRequest;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.CancelOrderRequestBody;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ChangeOrderResponse;
import org.jeecg.modules.business.domain.api.mabang.getshoplist.GetShopListRequest;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.Shop;
import org.jeecg.modules.business.service.DisabledShopSyncService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quartz class: org.jeecg.modules.business.domain.job.MabangDisabledShopSyncJob
 * No parameters. Configure its schedule in Quartz task management.
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class MabangDisabledShopSyncJob implements Job {
    @Autowired
    private DisabledShopSyncService syncService;
    @Autowired
    private IPlatformOrderService orderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Set<String> names = new GetShopListRequest().send().getDisabledShopNames();
            log.info("Currently disabled shops : {}", names);
            List<Shop> shops = syncService.deactivate(names);
            log.info("Disabled shops : {}", shops.stream().map(Shop::getErpCode).collect(Collectors.toList()));
            int success = 0;
            int failed = 0;
            if (!shops.isEmpty()) {
                List<String> shopIds = shops.stream().map(Shop::getId).distinct().collect(Collectors.toList());
                List<String> platformOrderIds = orderService.lambdaQuery()
                        .select(PlatformOrder::getId, PlatformOrder::getPlatformOrderId)
                        .in(PlatformOrder::getShopId, shopIds)
                        .in(PlatformOrder::getErpStatus, 1, 2)
                        .list()
                        .stream()
                        .map(PlatformOrder::getPlatformOrderId)
                        .collect(Collectors.toList());

                log.info("{} orders to be cancelled", platformOrderIds.size());
                for (List<String> batch : Lists.partition(new ArrayList<>(platformOrderIds), 10)) {
                    try {
                        ChangeOrderResponse response = new CancelOrderRequest(new CancelOrderRequestBody(
                                batch, "店铺已停用")).send();
                        if (response != null && response.success()) {
                            success += batch.size();
                        } else {
                            failed += batch.size();
                            log.error("Disabled shop order cancellation batch failed: orders={}, response={}",
                                    batch, response);
                        }
                    } catch (Exception ex) {
                        failed += batch.size();
                        log.error("Disabled shop order cancellation batch failed: orders={}", batch, ex);
                    }
                }
            }
            // Existing order synchronization remains authoritative for local erp_status.
            log.info("Disabled shop sync complete, cancelled orders : success={}, failures={}", success, failed);
            if (failed > 0) throw new JobExecutionException("Failed to cancel " + failed + " orders; retry next run");
        } catch (JobExecutionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JobExecutionException("Mabang disabled shop synchronization failed", ex);
        }
    }
}