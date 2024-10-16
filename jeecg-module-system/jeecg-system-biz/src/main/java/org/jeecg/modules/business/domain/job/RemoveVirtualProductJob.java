package org.jeecg.modules.business.domain.job;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.*;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.*;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class RemoveVirtualProductJob implements Job {

    private static final Integer DEFAULT_NUMBER_OF_DAYS = 5;

    private static final Integer DEFAULT_NUMBER_OF_THREADS = 10;
    private static final String REMOVED_SKU_STATUS = "4";;
    private Map<String, List<String>> virtualSkusByShop = new HashMap<>();

    @Autowired
    private IPlatformOrderService platformOrderService;
    @Autowired
    private IPlatformOrderMabangService platformOrderMabangService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDateTime endDateTime = LocalDateTime.now();
        LocalDateTime startDateTime = endDateTime.minusDays(DEFAULT_NUMBER_OF_DAYS);
        List<String> shops = new ArrayList<>();
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
                if (!jsonObject.isNull("virtualSkusByShop")) {
                    JSONArray virtualSkusByShopArray = jsonObject.getJSONArray("virtualSkusByShop");
                    for (int i = 0; i < virtualSkusByShopArray.length(); i++) {
                        JSONObject object = virtualSkusByShopArray.getJSONObject((i));
                        if (!object.isNull("shop")) {
                            String shopCode = object.getString("shop");
                            shops.add(shopCode);
                            if (!object.isNull("virtualSkus")) {
                                JSONArray virtualSkusArray = object.getJSONArray("virtualSkus");
                                List<String> virtualSkus = new ArrayList<>();
                                for (int j = 0; j < virtualSkusArray.length(); j++) {
                                    virtualSkus.add(virtualSkusArray.getString(j));
                                }
                                virtualSkusByShop.put(shopCode, virtualSkus);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                log.error("Error while parsing parameter as JSON, falling back to default parameters.");
            }
        }

        if (!endDateTime.isAfter(startDateTime)) {
            throw new RuntimeException("EndDateTime must be strictly greater than StartDateTime !");
        }

        List<PlatformOrder> platformOrders = platformOrderService.fetchUninvoicedOrdersForShops(startDateTime, endDateTime, shops);
        List<String> platformOrderIds = platformOrders.stream().map(PlatformOrder::getPlatformOrderId).collect(Collectors.toList());
        List<List<String>> platformOrderIdLists = Lists.partition(platformOrderIds, 10);

        List<OrderListRequestBody> requests = new ArrayList<>();
        for (List<String> platformOrderIdList : platformOrderIdLists) {
            requests.add(new OrderListRequestBody().setPlatformOrderIds(platformOrderIdList));
        }
        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_NUMBER_OF_THREADS);
        List<Order> mabangOrders = platformOrderMabangService.getOrdersFromMabang(requests, executor);

        log.info("Constructing virtual SKU removal requests");
        List<Order> ordersWithLogistic = new ArrayList<>();
        List<RemoveSkuRequestBody> removeSkuRequests = new ArrayList<>();
        Set<String> shopErpCodes = virtualSkusByShop.keySet();
        for (Order mabangOrder : mabangOrders) {
            String shopErpCode = mabangOrder.getShopErpCode();
            if (shopErpCodes.contains(shopErpCode)) {
                List<String> virtualSkus = virtualSkusByShop.get(shopErpCode);
                HashSet<Pair<String, Integer>> virtualSkuToRemove = new HashSet<>();
                for (OrderItem orderItem : mabangOrder.getOrderItems()) {
                    String skuErpCode = orderItem.getErpCode();
                    if (!orderItem.getStatus().equalsIgnoreCase(REMOVED_SKU_STATUS) && virtualSkus.contains(skuErpCode)) {
                        virtualSkuToRemove.add(Pair.of(skuErpCode, orderItem.getQuantity()));
                    }
                }
                if (!virtualSkuToRemove.isEmpty()) {
                    if(!mabangOrder.getLogisticChannelName().isEmpty()) {
                        ordersWithLogistic.add(mabangOrder);
                    }
                    RemoveSkuRequestBody removeSkuRequestBody = new RemoveSkuRequestBody(mabangOrder.getPlatformOrderId(),
                            virtualSkuToRemove);
                    removeSkuRequests.add(removeSkuRequestBody);
                }
            }
        }
        log.info("{} virtual SKU removal requests to be sent to MabangAPI", removeSkuRequests.size());

        // First we delete the logistic channel names, otherwise we can't delete virtual skus
        platformOrderMabangService.clearLogisticChannel(ordersWithLogistic, executor);

        List<CompletableFuture<Boolean>> removeSkuFutures = removeSkuRequests.stream()
                .map(removeSkuRequestBody -> CompletableFuture.supplyAsync(() -> {
                    boolean success = false;
                    try {
                        RemoveSkuRequest changeOrderRequest = new RemoveSkuRequest(removeSkuRequestBody);
                        ChangeOrderResponse response = changeOrderRequest.send();
                        success = response.success();
                    } catch (RuntimeException e) {
                        log.error("Error communicating with MabangAPI", e);
                    }
                    return success;
                }, executor))
                .collect(Collectors.toList());
        List<Boolean> results = removeSkuFutures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        long nbSuccesses = results.stream().filter(b -> b).count();
        log.info("{}/{} virtual SKU removal requests have succeeded.", nbSuccesses, removeSkuRequests.size());
    }
}
