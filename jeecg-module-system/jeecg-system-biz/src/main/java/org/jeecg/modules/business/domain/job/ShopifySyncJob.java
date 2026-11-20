package org.jeecg.modules.business.domain.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.api.shopify.*;
import org.jeecg.modules.business.entity.PlatformOrderShopSync;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ShopifySyncJob implements Job {

    private static final List<String> DEFAULT_INCLUDED_SHOPS = Arrays.asList("JCH3", "JCH4", "JCH5");
    private static final Integer DEFAULT_NUMBER_OF_THREADS = 10;
    private static final Integer SHOPIFY_API_RATE_LIMIT_PER_SHOP_PER_MINUTE = 40;

    @Autowired
    private IPlatformOrderService platformOrderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<String> shops = DEFAULT_INCLUDED_SHOPS;
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String parameter = ((String) jobDataMap.get("parameter"));
        if (parameter != null) {
            try {
                JSONObject jsonObject = new JSONObject(parameter);
                if (!jsonObject.isNull("includedShops")) {
                    JSONArray shopsArray = jsonObject.getJSONArray("includedShops");
                    List<String> shopList = new ArrayList<>();
                    for (int i = 0; i < shopsArray.length(); i++) {
                        shopList.add(shopsArray.getString(i));
                    }
                    shops = shopList;
                }
            } catch (JSONException e) {
                log.error("Error while parsing parameter as JSON, falling back to default parameters.");
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        List<PlatformOrderShopSync> ordersReadyForShopifySync = platformOrderService.fetchOrderInShopsReadyForShopifySync(shops);
        // Keep the original local order attached to each request, including split-order suffixes.
        Map<CreateFulfillmentRequestBody, String> sourceOrderIds = new ConcurrentHashMap<>();

        ExecutorService throttlingExecutorService = ThrottlingExecutorService.createExecutorService(DEFAULT_NUMBER_OF_THREADS,
                SHOPIFY_API_RATE_LIMIT_PER_SHOP_PER_MINUTE, TimeUnit.MINUTES);
        log.info("Constructing fulfillment retrieval requests");
        log.info("{} fulfillment retrieval requests to be sent to ShopifyAPI", ordersReadyForShopifySync.size());

        List<CompletableFuture<Boolean>> futures = ordersReadyForShopifySync.stream()
                .map(order -> CompletableFuture.supplyAsync(() -> {
                    boolean success = false;
                    try {
                        String responseStr = fetchFulfillmentOrders(new GetFulfillmentRequestBody(order));
                        FulfillmentOrdersResponse response = mapper.readValue(responseStr, FulfillmentOrdersResponse.class);
                        for (FulfillmentOrder fulfillmentOrder : response.getFulfillmentOrders()) {
                            if (fulfillmentOrder.isOpen()) {
                                sourceOrderIds.put(new CreateFulfillmentRequestBody(fulfillmentOrder.getId(), order),
                                        order.getPlatformOrderId());
                            } else {
                                log.info("Order {} is already fulfilled, skipping.", order.getPlatformOrderId());
                            }
                        }
                        success = true;
                    } catch (RuntimeException e) {
                        log.error("Error communicating with ShopifyAPI", e);
                    } catch (JsonProcessingException e) {
                        log.error("Error processing json", e);
                    }
                    return success;
                }, throttlingExecutorService))
                .collect(Collectors.toList());
        List<Boolean> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        long nbSuccesses = results.stream().filter(b -> b).count();
        log.info("{}/{} fulfillment retrieval requests have succeeded.", nbSuccesses, ordersReadyForShopifySync.size());

        log.info("Constructing fulfillment creation requests");
        List<CreateFulfillmentRequestBody> createFulfillmentRequests = new ArrayList<>(sourceOrderIds.keySet());
        log.info("{} fulfillment creation requests to be sent to ShopifyAPI", createFulfillmentRequests.size());

        Set<String> syncedPlatformOrderIds = ConcurrentHashMap.newKeySet();
        List<CompletableFuture<Boolean>> fulfillmentCreationFutures = createFulfillmentRequests.stream()
                .map(changeOrderRequestBody -> CompletableFuture.supplyAsync(() -> {
                    boolean success = false;
                    try {
                        String responseStr = createFulfillment(changeOrderRequestBody);
                        FulfillmentCreationResponse response = mapper.readValue(responseStr, FulfillmentCreationResponse.class);
                        success = response.getFulfillment().isSuccess();
                        if (success) {
                            syncedPlatformOrderIds.add(sourceOrderIds.get(changeOrderRequestBody));
                        }
                    } catch (RuntimeException e) {
                        log.error("Error communicating with ShopifyAPI", e);
                    } catch (JsonProcessingException e) {
                        log.error("Error processing json", e);
                    }
                    return success;
                }, throttlingExecutorService))
                .collect(Collectors.toList());
        results = fulfillmentCreationFutures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        throttlingExecutorService.shutdown();
        nbSuccesses = results.stream().filter(b -> b).count();
        log.info("{}/{} fulfillment creation requests have succeeded.", nbSuccesses, createFulfillmentRequests.size());

        if (!syncedPlatformOrderIds.isEmpty()) {
            platformOrderService.updateShopifySynced(syncedPlatformOrderIds);
            log.info("Those orders have been marked as shopify synced : {} ", syncedPlatformOrderIds);
        }
    }

    protected String fetchFulfillmentOrders(GetFulfillmentRequestBody body) {
        return new GetFulfillmentRequest(body).rawSend().getBody();
    }

    protected String createFulfillment(CreateFulfillmentRequestBody body) {
        return new CreateFulfillmentRequest(body).rawSend().getBody();
    }
}
