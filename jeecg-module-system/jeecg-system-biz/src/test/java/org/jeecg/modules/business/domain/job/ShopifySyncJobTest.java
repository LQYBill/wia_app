package org.jeecg.modules.business.domain.job;

import org.jeecg.modules.business.domain.api.shopify.CreateFulfillmentRequestBody;
import org.jeecg.modules.business.domain.api.shopify.GetFulfillmentRequestBody;
import org.jeecg.modules.business.entity.PlatformOrderShopSync;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ShopifySyncJobTest {

    @Test
    void lookupRemovesOnlyTrailingNumericSuffix() {
        for (String orderId : Arrays.asList("123", "123_1", "123_2", "123_12")) {
            assertEquals("orders/123/fulfillment_orders.json",
                    new GetFulfillmentRequestBody("shop", orderId, "token").endpoint());
        }
        assertEquals("orders/order_name/fulfillment_orders.json",
                new GetFulfillmentRequestBody("shop", "order_name", "token").endpoint());
    }

    @Test
    void splitOrdersKeepTheirTrackingNumbersAndOriginalIds() throws Exception {
        IPlatformOrderService service = mock(IPlatformOrderService.class);
        when(service.fetchOrderInShopsReadyForShopifySync(anyList())).thenReturn(Arrays.asList(
                order("123", "TRACK0"), order("123_1", "TRACK1"), order("123_2", "TRACK2")));
        FakeJob job = new FakeJob();
        ReflectionTestUtils.setField(job, "platformOrderService", service);

        job.execute(context());

        assertEquals(Collections.singleton("orders/123/fulfillment_orders.json"), job.endpoints);
        assertEquals(new HashSet<>(Arrays.asList("TRACK0", "TRACK1", "TRACK2")), job.trackingNumbers);
        verify(service).updateShopifySynced(new HashSet<>(Arrays.asList("123", "123_1", "123_2")));
    }

    @Test
    void failedSplitOrderIsNotMarkedAsSynced() throws Exception {
        IPlatformOrderService service = mock(IPlatformOrderService.class);
        when(service.fetchOrderInShopsReadyForShopifySync(anyList())).thenReturn(Arrays.asList(
                order("123_1", "TRACK1"), order("123_2", "FAIL")));
        FakeJob job = new FakeJob();
        ReflectionTestUtils.setField(job, "platformOrderService", service);

        job.execute(context());

        verify(service).updateShopifySynced(Collections.singleton("123_1"));
    }

    private PlatformOrderShopSync order(String id, String trackingNumber) {
        return new PlatformOrderShopSync(id, trackingNumber, "shop", "token", "75001");
    }

    private JobExecutionContext context() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());
        return context;
    }

    private static class FakeJob extends ShopifySyncJob {
        private final Set<String> endpoints = ConcurrentHashMap.newKeySet();
        private final Set<String> trackingNumbers = ConcurrentHashMap.newKeySet();

        @Override
        protected String fetchFulfillmentOrders(GetFulfillmentRequestBody body) {
            endpoints.add(body.endpoint());
            return "{\"fulfillment_orders\":[{\"id\":456,\"order_id\":123,\"status\":\"open\"}]}";
        }

        @Override
        protected String createFulfillment(CreateFulfillmentRequestBody body) {
            trackingNumbers.add(body.getTrackingNumber());
            if ("FAIL".equals(body.getTrackingNumber())) {
                throw new IllegalStateException("Simulated Shopify failure");
            }
            return "{\"fulfillment\":{\"order_id\":123,\"status\":\"success\"}}";
        }
    }
}
