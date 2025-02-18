package org.jeecg.modules.business.domain.job;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.Order;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.OrderListRequestBody;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ClearLogisticChannelJob implements Job {
    @Autowired
    private IPlatformOrderMabangService platformOrderMabangService;

    private static final Integer DEFAULT_NUMBER_OF_THREADS = 10;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("ClearLogisticChannelJob is executing ...");
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String parameter = ((String) jobDataMap.get("parameter"));
        List<String> platformOrderIds = new ArrayList<>();
        if (parameter != null) {
            try {
                JSONObject jsonObject = new JSONObject(parameter);
                if (!jsonObject.isNull("platformOrderIds")) {
                    JSONArray orderIds = jsonObject.getJSONArray("platformOrderIds");
                    if(orderIds == null) {
                        throw new RuntimeException("Empty parameter");
                    }
                    for(int i = 0; i < orderIds.length(); i++) {
                        platformOrderIds.add(orderIds.get(i).toString());
                    }
                }
                else {
                    throw new RuntimeException("platformOrderIds parameter is mandatory.");
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        List<List<String>> platformOrderIdLists = Lists.partition(platformOrderIds, 10);

        List<OrderListRequestBody> requests = new ArrayList<>();
        for (List<String> platformOrderIdList : platformOrderIdLists) {
            requests.add(new OrderListRequestBody().setPlatformOrderIds(platformOrderIdList));
        }
        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_NUMBER_OF_THREADS);
        List<Order> mabangOrders = platformOrderMabangService.getOrdersFromMabang(requests, executor);

        platformOrderMabangService.clearLogisticChannel(mabangOrders, executor);
        executor.shutdown();
        log.info("ClearLogisticChannelJob is finished.");
    }
}
