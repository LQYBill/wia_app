package org.jeecg.modules.business.domain.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.service.impl.WhopShipmentSyncService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;

/** Independent Mabang -> Whop carrier tracking upload job. */
@Slf4j
@DisallowConcurrentExecution
public class WhopShipmentSyncJob implements Job {
    @Autowired private WhopShipmentSyncService service;

    @Override public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JSONObject params = JSON.parseObject(context.getMergedJobDataMap().getString("parameter"));
            if (params == null) throw new IllegalArgumentException("Whop shipment job requires JSON parameters");
            String shop = params.getString("shopName");
            Integer batch = params.getInteger("batchSize");
            service.sync(params.getString("accountId"), shop == null ? "TID-Whop" : shop,
                    batch == null ? 100 : batch, params.getBooleanValue("dryRun"));
        } catch (Exception e) {
            Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
            String reason = root.getClass().getSimpleName() + ": " + String.valueOf(root.getMessage())
                    .replace('\r', ' ').replace('\n', ' ');
            if (reason.length() > 800) reason = reason.substring(0, 800);
            log.error("Whop shipment sync failed: {}", reason, e);
            throw new JobExecutionException("Whop shipment sync failed: " + reason, e);
        }
    }
}
