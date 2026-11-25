package org.jeecg.modules.business.domain.job;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ChangeOrderRequest;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ChangeOrderRequestBody;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ChangeOrderResponse;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ClearLogisticRequest;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ClearLogisticRequestBody;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.Order;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.OrderItem;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.OrderListRequestBody;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.OrderStatus;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper.JebKitOrderLine;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@DisallowConcurrentExecution
public class JebKitSplitJob implements Job {

    private static final String SHOP_CODE = "JEB";
    private static final String BUNDLE_SKU_ID = "54e2c09b-cab5-45a4-bf09-5f4e1d96e569";
    private static final Pattern KIT_FIELD = Pattern.compile("Kit\\s+(\\d+)(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRODUCT_WORD = Pattern.compile(
            "\\b(DRAGON|CAT|COW|DOG|DUCK|FOX|FROG|GIRAFFE|KOALA|MONKEY|OCTOPUS|PANDA|PENGUIN|PIG|"
                    + "RABBIT|SANTA|SNOWMAN|TURTLE|WHALE)\\b", Pattern.CASE_INSENSITIVE);

    @Autowired
    private PlatformOrderContentMapper orderContentMapper;
    @Autowired
    private IPlatformOrderMabangService platformOrderMabangService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String parameter = context.getMergedJobDataMap().getString("parameter");
        String orderId = null;
        if (StringUtils.isNotBlank(parameter)) {
            try {
                orderId = StringUtils.trimToNull(new JSONObject(parameter).getString("platformOrderId"));
                if (orderId == null) {
                    throw new JobExecutionException("platformOrderId is blank");
                }
            } catch (JSONException e) {
                throw new JobExecutionException("Expected a Quartz parameter with platformOrderId", e);
            }
        }

        Map<String, List<JebKitOrderLine>> candidates = orderContentMapper
                .findJebKitOrderLines(SHOP_CODE, BUNDLE_SKU_ID, OrderStatus.Pending.getCode(),
                        OrderStatus.Obsolete.getCode(), orderId)
                .stream().collect(Collectors.groupingBy(JebKitOrderLine::getOrderId));
        if (candidates.isEmpty()) {
            return;
        }

        List<OrderListRequestBody> requests = Lists.partition(new ArrayList<>(candidates.keySet()), 10).stream()
                .map(ids -> new OrderListRequestBody().setPlatformOrderIds(ids))
                .collect(Collectors.toList());
        // A split can use two API calls in addition to the batched order lookup.
        ExecutorService executor = ThrottlingExecutorService.createExecutorService(2, 100, TimeUnit.MINUTES);
        try {
            Map<String, List<Order>> currentOrders = platformOrderMabangService.getOrdersFromMabang(requests, executor)
                    .stream().collect(Collectors.groupingBy(Order::getPlatformOrderId));
            List<CompletableFuture<Boolean>> results = candidates.entrySet().stream()
                    .map(entry -> CompletableFuture.supplyAsync(
                            () -> splitOrder(entry.getKey(), entry.getValue(), currentOrders.get(entry.getKey())),
                            executor))
                    .collect(Collectors.toList());
            long failures = results.stream().map(CompletableFuture::join).filter(success -> !success).count();
            if (failures > 0) {
                throw new JobExecutionException(failures + " JEB kit orders could not be processed");
            }
        } finally {
            executor.shutdown();
        }
    }

    private boolean splitOrder(String orderId, List<JebKitOrderLine> lines, List<Order> currentOrders) {
        try {
            if (currentOrders == null || currentOrders.size() != 1) {
                throw new IllegalStateException("Mabang did not return exactly one matching order");
            }
            splitOrder(currentOrders.get(0), lines);
            return true;
        } catch (RuntimeException e) {
            log.warn("JEB kit split failed for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    void splitOrder(Order order, List<JebKitOrderLine> lines) {
        if (!SHOP_CODE.equals(order.getShopErpCode())
                || order.getStatus() == null || order.getIsNewOrder() == null || !order.isPending()) {
            return;
        }
        ChangeOrderRequestBody change = buildChange(order, lines);
        if (change == null) {
            return;
        }
        if (requiresChannelClear(order) && !clearChannel(order.getPlatformOrderId())) {
            throw new IllegalStateException("Mabang rejected the logistics channel clear request");
        }
        changeOrder(change);
        log.info("Split JEB kit for order {}", order.getPlatformOrderId());
    }

    private static boolean requiresChannelClear(Order order) {
        return order.hasLogisticChannelAssigned()
                || StringUtils.isNotBlank(order.getTrackingNumber())
                || StringUtils.isNotBlank(order.getTrackingNumber1());
    }

    boolean clearChannel(String orderId) {
        return new ClearLogisticRequest(new ClearLogisticRequestBody(orderId)).send().success();
    }

    void changeOrder(ChangeOrderRequestBody change) {
        ChangeOrderResponse response = new ChangeOrderRequest(change).sendOnce();
        if (!response.success()) {
            throw new IllegalStateException("Mabang rejected the SKU change: " + response.getMessage());
        }
    }

    static ChangeOrderRequestBody buildChange(Order order, List<JebKitOrderLine> lines) {
        String skuCode = lines.get(0).getSkuCode();
        if (StringUtils.isBlank(skuCode) || order.getOrderItems() == null) {
            throw new IllegalStateException("Missing bundle SKU or Mabang order items");
        }

        List<OrderItem> activeBundles = order.getOrderItems().stream()
                .filter(item -> skuCode.equals(item.getErpCode()) && !"4".equals(item.getStatus()))
                .collect(Collectors.toList());
        // The local sync may still contain the old line after a successful split.
        if (activeBundles.isEmpty()) {
            return null;
        }

        Multiset<Pair<String, Integer>> expectedLines = HashMultiset.create();
        Map<String, Integer> targetQuantities = new HashMap<>();
        for (JebKitOrderLine line : lines) {
            expectedLines.add(Pair.of(StringUtils.trimToEmpty(line.getCustomizationData()), line.getQuantity()));
            addKitProducts(line.getCustomizationData(), line.getQuantity(), targetQuantities);
        }

        Set<String> itemIds = new HashSet<>();
        HashSet<Triple<String, String, Integer>> removals = new HashSet<>();
        for (OrderItem item : activeBundles) {
            String itemId = item.getErpOrderItemId();
            if (StringUtils.isBlank(item.getStatus()) || StringUtils.isBlank(itemId) || !itemIds.add(itemId)
                    || !expectedLines.remove(Pair.of(StringUtils.trimToEmpty(item.getSpecifics()), item.getQuantity()))) {
                throw new IllegalStateException("Local and Mabang bundle lines do not match");
            }
            removals.add(Triple.of(skuCode, itemId, item.getQuantity()));
        }
        if (!expectedLines.isEmpty()) {
            throw new IllegalStateException("Local and Mabang bundle lines do not match");
        }

        HashSet<Pair<String, Integer>> additions = new HashSet<>();
        targetQuantities.forEach((sku, quantity) -> additions.add(Pair.of(sku, quantity)));
        return new ChangeOrderRequestBody(order.getPlatformOrderId(), null, removals, additions, null);
    }

    private static void addKitProducts(String customizationData, Integer quantity, Map<String, Integer> quantities) {
        if (StringUtils.isBlank(customizationData) || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Missing kit selections or invalid bundle quantity");
        }
        Set<Integer> slots = new HashSet<>();
        for (String attribute : customizationData.split(";")) {
            Matcher field = KIT_FIELD.matcher(attribute.trim());
            if (!field.matches()) {
                continue;
            }
            int slot = Integer.parseInt(field.group(1));
            String selection = field.group(2).trim();
            if (slot < 1 || !selection.startsWith(":") || !slots.add(slot)) {
                throw new IllegalArgumentException("Invalid or repeated Kit " + slot);
            }
            Matcher product = PRODUCT_WORD.matcher(selection.substring(1));
            if (!product.find()) {
                throw new IllegalArgumentException("Unknown product in Kit " + slot);
            }
            String productName = product.group().toUpperCase(Locale.ROOT);
            while (product.find()) {
                if (!productName.equalsIgnoreCase(product.group())) {
                    throw new IllegalArgumentException("Ambiguous product in Kit " + slot);
                }
            }
            String sku = "DRAGON".equals(productName) ? "WZ1010RDRAGON-JEB" : "WZ101" + productName + "-JEB";
            quantities.merge(sku, quantity, Math::addExact);
        }
        if (slots.isEmpty() || slots.stream().anyMatch(slot -> slot > slots.size())) {
            throw new IllegalArgumentException("Missing kit selections or nonconsecutive Kit numbers");
        }
    }
}
