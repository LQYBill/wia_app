package org.jeecg.modules.business.domain.job;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.*;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class AddPortraitTubeJob implements Job {

    private static final Integer DEFAULT_NUMBER_OF_DAYS = 5;

    private static final List<String> DEFAULT_SHOPS = Arrays.asList("JCH3", "JCH4", "JCH5");
    private static final Integer DEFAULT_NUMBER_OF_THREADS = 10;
    private static final String TUBE_40_SKU_SINGLE = "PJ95430032-WIA";
    private static final String TUBE_40_SKU_MULTIPLE = "PJ95430040-WIA";
    private static final String TUBE_50_SKU_SINGLE = "PJ95530032-WIA";
    private static final String TUBE_50_SKU_MULTIPLE = "PJ95530040-WIA";
    private static final List<String> TUBE_SKUS = Arrays.asList(TUBE_50_SKU_MULTIPLE, TUBE_50_SKU_SINGLE,
            TUBE_40_SKU_MULTIPLE, TUBE_40_SKU_SINGLE);
    private static final String PREFIX_50_CANVAS = "JJ2501";
    private static final String PREFIX_40_CANVAS = "JJ2500";

    @Autowired
    private IPlatformOrderService platformOrderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDateTime endDateTime = LocalDateTime.now();
        LocalDateTime startDateTime = endDateTime.minusDays(DEFAULT_NUMBER_OF_DAYS);
        List<String> shops = DEFAULT_SHOPS;
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String parameter = ((String) jobDataMap.get("parameter"));
        if (parameter != null) {
            try {
                JSONObject jsonObject = new JSONObject(parameter);
                if (!jsonObject.isNull("startDateTime")) {
                    String startDateStr = jsonObject.getString("startDateTime");
                    startDateTime = LocalDateTime.parse(startDateStr);
                }
                if (!jsonObject.isNull("endDateTime")) {
                    String endDateStr = jsonObject.getString("endDateTime");
                    endDateTime = LocalDateTime.parse(endDateStr);
                }
                if (!jsonObject.isNull("shops")) {
                    JSONArray shopsArray = jsonObject.getJSONArray("shops");
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

        if (!endDateTime.isAfter(startDateTime)) {
            throw new RuntimeException("EndDateTime must be strictly greater than StartDateTime !");
        }

        List<String> platformOrderIds = platformOrderService.fetchUninvoicedOrdersForShops(startDateTime, endDateTime, shops);
        List<List<String>> platformOrderIdLists = Lists.partition(platformOrderIds, 10);

        List<OrderListRequestBody> requests = new ArrayList<>();
        for (List<String> platformOrderIdList : platformOrderIdLists) {
            requests.add(new OrderListRequestBody().setPlatformOrderIds(platformOrderIdList));
        }
        List<Order> mabangOrders = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_NUMBER_OF_THREADS);
        List<CompletableFuture<Boolean>> futures = requests.stream()
                .map(request -> CompletableFuture.supplyAsync(() -> {
                    boolean success = false;
                    try {
                        OrderListRawStream rawStream = new OrderListRawStream(request);
                        OrderListStream stream = new OrderListStream(rawStream);
                        mabangOrders.addAll(stream.all());
                        success = true;
                    } catch (RuntimeException e) {
                        log.error("Error communicating with MabangAPI", e);
                    }
                    return success;
                }, executor))
                .collect(Collectors.toList());
        List<Boolean> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        long nbSuccesses = results.stream().filter(b -> b).count();
        log.info("{}/{} requests have succeeded.", nbSuccesses, requests.size());
        log.info("{}/{} mabang orders have been retrieved.", mabangOrders.size(), platformOrderIds.size());

        for (Order mabangOrder : mabangOrders) {

        }
    }

    private Pair<String, Integer> findAdequateTubeAndQuantity(List<OrderItem> orderItems) {
        int canvasCount = 0;
        boolean need50Tube = false;
        Pair<String, Integer> currentTube = null;
        for (OrderItem orderItem : orderItems) {
            String sku = orderItem.getErpCode();
            int quantity = orderItem.getQuantity();
            if (currentTube == null && TUBE_SKUS.contains(sku)) {
                currentTube = Pair.of(sku, quantity);
            }
            if (sku.startsWith(PREFIX_50_CANVAS)) {
                need50Tube = true;
                canvasCount += quantity;
            } else if (sku.startsWith(PREFIX_40_CANVAS)) {
                canvasCount += quantity;
            }
        }
        Pair<String, Integer> adequateTube;
        if (need50Tube) {
            if (canvasCount > 1) {
                adequateTube = Pair.of(TUBE_50_SKU_MULTIPLE, canvasCount);
            } else {
                adequateTube = Pair.of(TUBE_50_SKU_SINGLE, canvasCount);
            }
        } else {
            if (canvasCount > 1) {
                adequateTube = Pair.of(TUBE_40_SKU_MULTIPLE, canvasCount);
            } else {
                adequateTube = Pair.of(TUBE_40_SKU_SINGLE, canvasCount);
            }
        }
        if (adequateTube.equals(currentTube)) {
            return null;
        }
        return currentTube;
    }
}
