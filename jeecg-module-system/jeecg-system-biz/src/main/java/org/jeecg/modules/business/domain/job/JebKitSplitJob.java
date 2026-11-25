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
import java.util.Objects;
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
                    + "RABBIT|BUNNY|SANTA|SNOWMAN|TURTLE|WHALE)\\b", Pattern.CASE_INSENSITIVE);

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
        candidates.entrySet().removeIf(entry -> {
            if (entry.getValue().stream().noneMatch(line -> StringUtils.isBlank(line.getCustomizationData()))) {
                return false;
            }
            log.warn("Skipping JEB kit order {}: missing kit selections in customization_data", entry.getKey());
            return true;
        });
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
            List<CompletableFuture<String>> results = candidates.entrySet().stream()
                    .map(entry -> CompletableFuture.supplyAsync(
                            () -> splitOrder(entry.getKey(), entry.getValue(), currentOrders.get(entry.getKey())),
                            executor))
                    .collect(Collectors.toList());
            List<String> failures = results.stream().map(CompletableFuture::join)
                    .filter(Objects::nonNull).collect(Collectors.toList());
            if (!failures.isEmpty()) {
                String examples = failures.stream().limit(10).collect(Collectors.joining("; "));
                throw new JobExecutionException(failures.size() + " JEB kit orders could not be processed: " + examples);
            }
        } finally {
            executor.shutdown();
        }
    }

    private String splitOrder(String orderId, List<JebKitOrderLine> lines, List<Order> currentOrders) {
        try {
            if (currentOrders == null || currentOrders.isEmpty()) {
                throw new IllegalStateException("Mabang order lookup returned no matching order");
            }
            if (currentOrders.size() != 1) {
                throw new IllegalStateException("Mabang order lookup returned " + currentOrders.size() + " matching orders");
            }
            splitOrder(currentOrders.get(0), lines);
            return null;
        } catch (RuntimeException e) {
            String reason = StringUtils.abbreviate(StringUtils.normalizeSpace(
                    StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName())), 240);
            log.warn("JEB kit split failed for order {}: {}", orderId, reason);
            return orderId + " (" + reason + ")";
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
            if (StringUtils.isBlank(item.getStatus())) {
                throw new IllegalStateException("Mabang bundle line is missing its status");
            }
            if (StringUtils.isBlank(itemId) || !itemIds.add(itemId)) {
                throw new IllegalStateException("Mabang bundle line ID is missing or duplicated");
            }
            if (!expectedLines.remove(Pair.of(StringUtils.trimToEmpty(item.getSpecifics()), item.getQuantity()))) {
                throw new IllegalStateException("Mabang bundle line attributes or quantity do not match local data");
            }
            removals.add(Triple.of(skuCode, itemId, item.getQuantity()));
        }
        if (!expectedLines.isEmpty()) {
            throw new IllegalStateException("Mabang is missing one or more active local bundle lines");
        }

        HashSet<Pair<String, Integer>> additions = new HashSet<>();
        targetQuantities.forEach((sku, quantity) -> additions.add(Pair.of(sku, quantity)));
        return new ChangeOrderRequestBody(order.getPlatformOrderId(), null, removals, additions, null);
    }

    private static void addKitProducts(String customizationData, Integer quantity, Map<String, Integer> quantities) {
        if (StringUtils.isBlank(customizationData)) {
            throw new IllegalArgumentException("Missing kit selections in customization_data");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Invalid bundle quantity");
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
            String productName = canonicalProductName(product.group());
            while (product.find()) {
                if (!productName.equals(canonicalProductName(product.group()))) {
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

    private static String canonicalProductName(String word) {
        return "BUNNY".equalsIgnoreCase(word) ? "RABBIT" : word.toUpperCase(Locale.ROOT);
    }
}
