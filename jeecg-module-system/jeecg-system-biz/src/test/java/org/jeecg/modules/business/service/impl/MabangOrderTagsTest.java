package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.Order;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.OrderStatus;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.mapper.PlatformOrderMabangMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MabangOrderTagsTest {
    private static final String SUBSCRIPTION_TAGS = "Subscription, Subscription Recurring Order";

    private final PlatformOrderMabangMapper mapper = mock(PlatformOrderMabangMapper.class);
    private final PlatformOrderMabangServiceImpl service = new PlatformOrderMabangServiceImpl();

    MabangOrderTagsTest() {
        ReflectionTestUtils.setField(service, "platformOrderMabangMapper", mapper);
    }

    @Test
    void insertsNewOrderWithTagsFromMabang() {
        Order order = JSON.parseObject("{\"platformOrderId\":\"new\",\"orderStatus\":\"2\",\"isNewOrder\":\"1\","
                + "\"hasGoods\":\"1\",\"orderItem\":[],\"platformOrderTags\":\"" + SUBSCRIPTION_TAGS + "\"}", Order.class);

        service.saveOrderFromMabang(new ArrayList<>(Collections.singletonList(order)));

        assertEquals(SUBSCRIPTION_TAGS, order.getPlatformOrderTags());
        verify(mapper).insertOrdersFromMabang(Collections.singletonList(order));
    }

    @Test
    void refreshesOnlyChangedTagsOnExistingShippedOrders() {
        Order updated = shippedOrder("updated", SUBSCRIPTION_TAGS);
        Order cleared = shippedOrder("cleared", "");
        Order missing = shippedOrder("missing", null);
        Order unchanged = shippedOrder("unchanged", SUBSCRIPTION_TAGS);
        when(mapper.searchExistence(Arrays.asList("updated", "cleared", "missing", "unchanged")))
                .thenReturn(Arrays.asList(storedOrder("updated", null), storedOrder("cleared", "Old tag"),
                        storedOrder("missing", "Keep tag"), storedOrder("unchanged", SUBSCRIPTION_TAGS)));

        service.saveOrderFromMabang(new ArrayList<>(Arrays.asList(updated, cleared, missing, unchanged)));

        assertEquals("local-updated", updated.getId());
        assertEquals("local-cleared", cleared.getId());
        verify(mapper).searchExistence(Arrays.asList("updated", "cleared", "missing", "unchanged"));
        verify(mapper).batchUpdateShopifyTags(Arrays.asList(updated, cleared));
        verifyNoMoreInteractions(mapper);
    }

    private static Order shippedOrder(String id, String tags) {
        Order order = new Order();
        order.setPlatformOrderId(id);
        order.setStatus(OrderStatus.Shipped.getCode());
        order.setPlatformOrderTags(tags);
        return order;
    }

    private static PlatformOrder storedOrder(String id, String tags) {
        PlatformOrder order = new PlatformOrder();
        order.setId("local-" + id);
        order.setPlatformOrderId(id);
        order.setErpStatus(OrderStatus.Shipped.getCode());
        order.setShopifyTags(tags);
        return order;
    }
}
