package org.jeecg.modules.business.domain.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ChangeOrderRequestBody;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.Order;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.OrderItem;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper.JebKitOrderLine;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JebKitSplitJobTest {

    private static final String ORDER_ID = "13377878131036";
    private static final String BUNDLE = "BUNDLE-JEB";
    private static final String COW_PENGUIN_DOG =
            "Kit 1:Glen The Highland Cow;Kit 2:Pinguy The Penguin;Kit 3:Alfie The Dog;";
    private static final String KOALA_SANTA_COW =
            "Kit 1:Kayle The Koala;Kit 2:Santa Claus;Kit 3:Glen The Highland Cow";

    @Test
    void splitsBothExampleSelectionsAndAggregatesQuantities() {
        OrderItem standaloneCow = item("standalone-cow", "WZ101COW-JEB", null, 1, "2");
        standaloneCow.setOriginTitle("Glen The Highland Cow");
        Order order = order(
                item("line-1", BUNDLE, COW_PENGUIN_DOG, 2, "2"),
                item("line-2", BUNDLE, KOALA_SANTA_COW, 1, "2"),
                standaloneCow,
                item("removed-bundle", BUNDLE, KOALA_SANTA_COW, 1, "4"));

        ChangeOrderRequestBody change = JebKitSplitJob.buildChange(order,
                Arrays.asList(line(COW_PENGUIN_DOG, 2), line(KOALA_SANTA_COW, 1)));

        Map<String, Integer> expectedAdditions = new HashMap<>();
        expectedAdditions.put("WZ101COW-JEB", 3);
        expectedAdditions.put("WZ101PENGUIN-JEB", 2);
        expectedAdditions.put("WZ101DOG-JEB", 2);
        expectedAdditions.put("WZ101KOALA-JEB", 1);
        expectedAdditions.put("WZ101SANTA-JEB", 1);
        assertEquals(expectedAdditions, addedQuantities(change));
    }

    @Test
    void addsOnlyMissingComponentsWhenBundleRemainsActive() {
        OrderItem bundle = item("bundle", BUNDLE, COW_PENGUIN_DOG, 2, "2");
        OrderItem cow = item("added-cow", "WZ101COW-JEB", null, 1, "2");
        OrderItem penguin = item("added-penguin", "WZ101PENGUIN-JEB", null, 2, "2");
        OrderItem removedDog = item("removed-dog", "WZ101DOG-JEB", null, 2, "4");
        Order order = order(bundle, cow, penguin, removedDog);

        ChangeOrderRequestBody change = JebKitSplitJob.buildChange(order,
                Collections.singletonList(line(COW_PENGUIN_DOG, 2)));

        Map<String, Integer> expected = new HashMap<>();
        expected.put("WZ101COW-JEB", 1);
        expected.put("WZ101DOG-JEB", 2);
        assertEquals(expected, addedQuantities(change));
    }

    @Test
    void doesNotChangeOrderAgainAfterAllComponentsWereAdded() {
        Order order = order(
                item("bundle", BUNDLE, COW_PENGUIN_DOG, 1, "2"),
                item("added-cow", "WZ101COW-JEB", null, 1, "2"),
                item("added-penguin", "WZ101PENGUIN-JEB", null, 1, "2"),
                item("added-dog", "WZ101DOG-JEB", null, 1, "2"));
        order.setLogisticChannelName("assigned channel");
        JebKitSplitJob job = spy(new JebKitSplitJob());

        job.splitOrder(order, Collections.singletonList(line(COW_PENGUIN_DOG, 1)));

        verify(job, never()).clearChannel(any());
        verify(job, never()).changeOrder(any());
    }

    @Test
    void mapsAllNineteenProducts() {
        String[] products = {"dragon", "cat", "cow", "dog", "duck", "fox", "frog", "giraffe", "koala",
                "monkey", "octopus", "panda", "penguin", "pig", "rabbit", "santa", "snowman", "turtle", "whale"};
        String[] skuCodes = {"WZ1010RDRAGON-JEB", "WZ101CAT-JEB", "WZ101COW-JEB", "WZ101DOG-JEB",
                "WZ101DUCK-JEB", "WZ101FOX-JEB", "WZ101FROG-JEB", "WZ101GIRAFFE-JEB", "WZ101KOALA-JEB",
                "WZ101MONKEY-JEB", "WZ101OCTOPUS-JEB", "WZ101PANDA-JEB", "WZ101PENGUIN-JEB", "WZ101PIG-JEB",
                "WZ101RABBIT-JEB", "WZ101SANTA-JEB", "WZ101SNOWMAN-JEB", "WZ101TURTLE-JEB", "WZ101WHALE-JEB"};
        for (int i = 0; i < products.length; i++) {
            String selection = "Kit 1: " + products[i];
            Order order = order(item("line-1", BUNDLE, selection, 1, "2"));
            ChangeOrderRequestBody change = JebKitSplitJob.buildChange(order, Collections.singletonList(line(selection, 1)));
            assertTrue(change.parameters().getString("stockData").contains(skuCodes[i]));
        }
    }

    @Test
    void mapsBunnyToRabbitSku() {
        String selection = "Kit 1:Tiki The Duck;Kit 2:Pinguy The Penguin;Kit 3:Luna The Bunny";
        Order order = order(item("line-1", BUNDLE, selection, 1, "2"));
        ChangeOrderRequestBody change = JebKitSplitJob.buildChange(order, Collections.singletonList(line(selection, 1)));
        assertTrue(change.parameters().getString("stockData").contains("WZ101RABBIT-JEB"));
    }

    @Test
    void ignoresRemovedBundleEvenWhenLocalCopyStillContainsIt() {
        Order order = order(item("line-1", BUNDLE, COW_PENGUIN_DOG, 1, "4"));
        assertNull(JebKitSplitJob.buildChange(order, Collections.singletonList(line(COW_PENGUIN_DOG, 1))));
    }

    @Test
    void refusesInvalidOrChangedSelections() {
        for (String selection : Arrays.asList("Kit 1: Dog;Kit 2: Meerkat", "Kit 1: Cat and Dog",
                "Kit 1: Cat;Kit 3: Dog", "Kit 1: Dog;Kit 2", "Kit 1: Dog;Kit 1: Cow")) {
            Order order = order(item("line-1", BUNDLE, selection, 1, "2"));
            assertThrows(IllegalArgumentException.class,
                    () -> JebKitSplitJob.buildChange(order, Collections.singletonList(line(selection, 1))));
        }

        Order order = order(item("line-1", BUNDLE, KOALA_SANTA_COW, 1, "2"));
        assertThrows(IllegalStateException.class,
                () -> JebKitSplitJob.buildChange(order, Collections.singletonList(line(COW_PENGUIN_DOG, 1))));
    }

    @Test
    void clearsAssignedChannelBeforeChangingSkus() {
        Order order = order(item("line-1", BUNDLE, COW_PENGUIN_DOG, 1, "2"));
        order.setLogisticChannelName("assigned channel");
        JebKitSplitJob job = spy(new JebKitSplitJob());
        doReturn(true).when(job).clearChannel(ORDER_ID);
        doNothing().when(job).changeOrder(any());

        job.splitOrder(order, Collections.singletonList(line(COW_PENGUIN_DOG, 1)));

        InOrder calls = inOrder(job);
        calls.verify(job).clearChannel(ORDER_ID);
        calls.verify(job).changeOrder(any());
    }

    @Test
    void clearFailurePreventsSkuChangeOnPendingOrderWithTracking() {
        Order order = order(item("line-1", BUNDLE, COW_PENGUIN_DOG, 1, "2"));
        order.setTrackingNumber("assigned tracking number");
        JebKitSplitJob job = spy(new JebKitSplitJob());
        doReturn(false).when(job).clearChannel(ORDER_ID);

        assertThrows(IllegalStateException.class,
                () -> job.splitOrder(order, Collections.singletonList(line(COW_PENGUIN_DOG, 1))));
        verify(job).clearChannel(ORDER_ID);
        verify(job, never()).changeOrder(any());
    }

    @Test
    void skipsOtherShopsAndNonPendingOrders() {
        Order order = order(item("line-1", BUNDLE, COW_PENGUIN_DOG, 1, "2"));
        order.setLogisticChannelName("assigned channel");
        JebKitSplitJob job = spy(new JebKitSplitJob());
        order.setShopErpCode("OTHER");
        job.splitOrder(order, Collections.singletonList(line(COW_PENGUIN_DOG, 1)));
        order.setShopErpCode("JEB");
        for (String status : Arrays.asList("1", "3", "4", "5", null)) {
            order.setStatus(status);
            job.splitOrder(order, Collections.singletonList(line(COW_PENGUIN_DOG, 1)));
        }
        order.setStatus("2");
        order.setIsNewOrder("2");
        job.splitOrder(order, Collections.singletonList(line(COW_PENGUIN_DOG, 1)));
        verify(job, never()).clearChannel(any());
        verify(job, never()).changeOrder(any());
    }

    @Test
    void skipsOrderWithBlankCustomizationWithoutCallingMabang() throws JobExecutionException {
        JebKitSplitJob job = new JebKitSplitJob();
        PlatformOrderContentMapper mapper = mock(PlatformOrderContentMapper.class);
        IPlatformOrderMabangService mabang = mock(IPlatformOrderMabangService.class);
        ReflectionTestUtils.setField(job, "orderContentMapper", mapper);
        ReflectionTestUtils.setField(job, "platformOrderMabangService", mabang);

        JebKitOrderLine missingSelections = line("  ", 1);
        missingSelections.setOrderId(ORDER_ID);
        JebKitOrderLine validSelections = line(COW_PENGUIN_DOG, 1);
        validSelections.setOrderId(ORDER_ID);
        when(mapper.findJebKitOrderLines("JEB", "54e2c09b-cab5-45a4-bf09-5f4e1d96e569", "1", "5", null))
                .thenReturn(Arrays.asList(missingSelections, validSelections));
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        job.execute(context);

        verify(mabang, never()).getOrdersFromMabang(any(), any());
    }

    @Test
    void includesOrderIdsAndDistinctLookupFailuresInQuartzSummary() {
        JebKitSplitJob job = new JebKitSplitJob();
        PlatformOrderContentMapper mapper = mock(PlatformOrderContentMapper.class);
        IPlatformOrderMabangService mabang = mock(IPlatformOrderMabangService.class);
        ReflectionTestUtils.setField(job, "orderContentMapper", mapper);
        ReflectionTestUtils.setField(job, "platformOrderMabangService", mabang);

        JebKitOrderLine missingOrder = line(COW_PENGUIN_DOG, 1);
        missingOrder.setOrderId("missing-order");
        JebKitOrderLine duplicateOrder = line(COW_PENGUIN_DOG, 1);
        duplicateOrder.setOrderId(ORDER_ID);
        when(mapper.findJebKitOrderLines("JEB", "54e2c09b-cab5-45a4-bf09-5f4e1d96e569", "1", "5", null))
                .thenReturn(Arrays.asList(missingOrder, duplicateOrder));
        when(mabang.getOrdersFromMabang(any(), any())).thenReturn(Arrays.asList(order(), order()));
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        String summary = assertThrows(JobExecutionException.class, () -> job.execute(context)).getMessage();
        assertTrue(summary.startsWith("2 JEB kit orders could not be processed"));
        assertTrue(summary.contains("missing-order (Mabang order lookup returned no matching order)"));
        assertTrue(summary.contains(ORDER_ID + " (Mabang order lookup returned 2 matching orders)"));
    }

    private static Map<String, Integer> addedQuantities(ChangeOrderRequestBody change) {
        Map<String, Integer> additions = new HashMap<>();
        for (Object value : JSON.parseArray(change.parameters().getString("stockData"))) {
            JSONObject operation = (JSONObject) value;
            assertEquals(3, operation.getIntValue("type"));
            additions.put(operation.getString("stockSku"), operation.getIntValue("quantity"));
        }
        return additions;
    }

    private static Order order(OrderItem... items) {
        Order order = new Order();
        order.setShopErpCode("JEB");
        order.setStatus("2");
        order.setIsNewOrder("1");
        order.setPlatformOrderId(ORDER_ID);
        order.setOrderItems(Arrays.asList(items));
        return order;
    }

    private static JebKitOrderLine line(String selections, int quantity) {
        JebKitOrderLine line = new JebKitOrderLine();
        line.setSkuCode(BUNDLE);
        line.setCustomizationData(selections);
        line.setQuantity(quantity);
        return line;
    }

    private static OrderItem item(String id, String sku, String selections, int quantity, String status) {
        OrderItem item = new OrderItem();
        item.setErpOrderItemId(id);
        item.setErpCode(sku);
        item.setSpecifics(selections);
        item.setQuantity(quantity);
        item.setStatus(status);
        return item;
    }
}
