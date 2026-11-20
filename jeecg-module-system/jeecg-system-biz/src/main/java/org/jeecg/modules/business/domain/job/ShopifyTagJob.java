package org.jeecg.modules.business.domain.job;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.api.mabang.orderDoOrderAbnormal.OrderSuspendRequest;
import org.jeecg.modules.business.domain.api.mabang.orderDoOrderAbnormal.OrderSuspendRequestBody;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@DisallowConcurrentExecution
public class ShopifyTagJob implements Job {

    private static final List<String> DEFAULT_INCLUDED_SHOPS = Collections.singletonList("AC");
    private static final List<String> DEFAULT_TAGS = Collections.singletonList("Subscription");
    private static final String DEFAULT_ABNORMAL_LABEL_NAME = "AC复购订单";
    private static final int MABANG_API_RATE_LIMIT_PER_MINUTE = 10;

    @Autowired
    private IPlatformOrderService platformOrderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<String> shops = DEFAULT_INCLUDED_SHOPS;
        List<String> tags = DEFAULT_TAGS;
        String labelName = DEFAULT_ABNORMAL_LABEL_NAME;
        String parameter = context.getMergedJobDataMap().getString("parameter");
        if (parameter != null && !parameter.trim().isEmpty()) {
            try {
                JSONObject json = new JSONObject(parameter);
                if (!json.isNull("includedShops")) {
                    shops = readValues(json.getJSONArray("includedShops"));
                }
                if (!json.isNull("tags")) {
                    tags = readValues(json.getJSONArray("tags"));
                }
                if (!json.isNull("abnormalLabelName")) {
                    labelName = json.getString("abnormalLabelName").trim();
                }
            } catch (JSONException e) {
                throw new JobExecutionException("Invalid ShopifyTagJob parameters", e);
            }
        }
        if (labelName.isEmpty()) {
            throw new JobExecutionException("abnormalLabelName must not be blank");
        }
        if (shops.isEmpty() || tags.isEmpty()) {
            return;
        }

        List<String> requiredTags = tags;
        List<PlatformOrder> matchingOrders = platformOrderService.fetchPlatformOrdersWithShopifyTags(shops).stream()
                .filter(order -> Arrays.stream(order.getShopifyTags().split(","))
                        .map(String::trim)
                        .anyMatch(requiredTags::contains))
                .collect(Collectors.toList());
        if (matchingOrders.isEmpty()) {
            return;
        }

        String reviewLabel = labelName;
        ExecutorService executor = ThrottlingExecutorService.createExecutorService(
                2, MABANG_API_RATE_LIMIT_PER_MINUTE, TimeUnit.MINUTES);
        try {
            List<CompletableFuture<PlatformOrder>> requests = matchingOrders.stream()
                    .map(order -> CompletableFuture.supplyAsync(() -> sendToReview(order, reviewLabel), executor))
                    .collect(Collectors.toList());
            List<PlatformOrder> reviewedOrders = requests.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!reviewedOrders.isEmpty() && !platformOrderService.updateBatchById(reviewedOrders)) {
                throw new JobExecutionException("Failed to record Shopify tag review results");
            }
            log.info("ShopifyTagJob moved {}/{} orders to Mabang review category {}.",
                    reviewedOrders.size(), matchingOrders.size(), reviewLabel);
        } finally {
            executor.shutdown();
        }
    }

    private static List<String> readValues(JSONArray array) throws JSONException {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String value = array.getString(i).trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private PlatformOrder sendToReview(PlatformOrder order, String labelName) {
        OrderSuspendRequestBody body = new OrderSuspendRequestBody(
                order.getPlatformOrderId(), labelName, order.getShopifyTags());
        try {
            if (new OrderSuspendRequest(body).send().success()) {
                PlatformOrder update = new PlatformOrder();
                update.setId(order.getId());
                update.setAlreadySetAbnormal("1");
                return update;
            }
            log.warn("Mabang rejected moving order {} to review category {}.", order.getPlatformOrderId(), labelName);
        } catch (RuntimeException e) {
            log.warn("Failed to move order {} to review category {}.", order.getPlatformOrderId(), labelName, e);
        }
        return null;
    }
}
