package org.jeecg.modules.business.domain.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.service.impl.WhopOrderPullService;
import org.jeecg.modules.business.service.impl.WhopOrderPushService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

/** Persist Whop details first, then create and reconcile Mabang orders. */
@Slf4j
@DisallowConcurrentExecution
public class WhopOrderSyncJob implements Job {
    @Autowired private WhopOrderPullService pull;
    @Autowired private WhopOrderPushService push;

    @Override public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JSONObject params = JSON.parseObject(context.getMergedJobDataMap().getString("parameter"));
            if (params == null) throw new IllegalArgumentException("Whop job requires JSON parameters");
            String account = params.getString("accountId");
            String start = params.getString("startTime");
            int fetched = pull.pull(account, start == null ? null : Instant.parse(start));
            int submitted = params.getBooleanValue("pullOnly") ? 0 : push.push(account);
            log.info("Whop sync persisted {}, submitted {} to Mabang", fetched, submitted);
        } catch (Exception e) {
            log.error("Whop sync failed ({})", e.getClass().getSimpleName());
            throw new JobExecutionException("Whop sync failed; inspect sync state and configuration");
        }
    }
}
