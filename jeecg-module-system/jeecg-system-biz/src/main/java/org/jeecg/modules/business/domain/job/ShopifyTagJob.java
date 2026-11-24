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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        String parameter = context.getMergedJobDataMap().getString("parameter");
        Map<PlatformOrder, String> matchingOrders = new LinkedHashMap<>();
        for (ReviewRule rule : readRules(parameter)) {
            if (!rule.shops.isEmpty() && !rule.tags.isEmpty()) {
                platformOrderService.fetchPlatformOrdersWithShopifyTags(rule.shops).stream()
                        .filter(order -> Arrays.stream(order.getShopifyTags().split(","))
                                .map(String::trim)
                                .anyMatch(rule.tags::contains))
                        .forEach(order -> matchingOrders.put(order, rule.labelName));
            }
        }
        if (matchingOrders.isEmpty()) {
            return;
        }

        ExecutorService executor = ThrottlingExecutorService.createExecutorService(
                2, MABANG_API_RATE_LIMIT_PER_MINUTE, TimeUnit.MINUTES);
        try {
            List<CompletableFuture<PlatformOrder>> requests = matchingOrders.entrySet().stream()
                    .map(entry -> CompletableFuture.supplyAsync(
                            () -> sendToReview(entry.getKey(), entry.getValue()), executor))
                    .collect(Collectors.toList());
            List<PlatformOrder> reviewedOrders = requests.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!reviewedOrders.isEmpty() && !platformOrderService.updateBatchById(reviewedOrders)) {
                throw new JobExecutionException("Failed to record Shopify tag review results");
            }
            log.info("ShopifyTagJob moved {}/{} matching orders to Mabang review.",
                    reviewedOrders.size(), matchingOrders.size());
        } finally {
            executor.shutdown();
        }
    }

    private static List<ReviewRule> readRules(String parameter) throws JobExecutionException {
        try {
            JSONObject json = parameter == null || parameter.trim().isEmpty()
                    ? new JSONObject() : new JSONObject(parameter);
            if (json.has("rules")) {
                return readShopRules(json.getJSONArray("rules"));
            }
            List<String> shops = json.isNull("includedShops")
                    ? DEFAULT_INCLUDED_SHOPS : readValues(json.getJSONArray("includedShops"));
            List<String> tags = json.isNull("tags") ? DEFAULT_TAGS : readValues(json.getJSONArray("tags"));
            String labelName = json.isNull("abnormalLabelName")
                    ? DEFAULT_ABNORMAL_LABEL_NAME : json.getString("abnormalLabelName").trim();
            return Collections.singletonList(new ReviewRule(shops, tags, labelName));
        } catch (JSONException e) {
            throw new JobExecutionException("Invalid ShopifyTagJob parameters", e);
        }
    }

    private static List<ReviewRule> readShopRules(JSONArray array) throws JSONException, JobExecutionException {
        List<ReviewRule> rules = new ArrayList<>();
        Set<String> shops = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject rule = array.getJSONObject(i);
            String shop = readRequiredString(rule, "shop");
            if (!shops.add(shop.toUpperCase(Locale.ROOT))) {
                throw new JobExecutionException("Duplicate Shopify tag rule for shop " + shop);
            }
            rules.add(new ReviewRule(Collections.singletonList(shop), readValues(rule.getJSONArray("tags")),
                    readRequiredString(rule, "abnormalLabelName")));
        }
        return rules;
    }

    private static String readRequiredString(JSONObject rule, String key) throws JSONException, JobExecutionException {
        Object value = rule.get(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw new JobExecutionException("Rule " + key + " must be a non-blank string");
        }
        return ((String) value).trim();
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

    private static final class ReviewRule {
        private final List<String> shops;
        private final List<String> tags;
        private final String labelName;

        private ReviewRule(List<String> shops, List<String> tags, String labelName) throws JobExecutionException {
            if (labelName.isEmpty()) {
                throw new JobExecutionException("abnormalLabelName must not be blank");
            }
            this.shops = shops;
            this.tags = tags;
            this.labelName = labelName;
        }
    }
}
