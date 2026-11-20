package org.jeecg.modules.business.domain.job;

import org.jeecg.common.util.RestUtil;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ShopifyTagJobTest {
    private final IPlatformOrderService orders = mock(IPlatformOrderService.class);
    private final ShopifyTagJob job = new ShopifyTagJob();
    private ClientHttpRequestFactory previousFactory;
    private MockRestServiceServer mabang;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "platformOrderService", orders);
        previousFactory = RestUtil.getRestTemplate().getRequestFactory();
        mabang = MockRestServiceServer.bindTo(RestUtil.getRestTemplate()).ignoreExpectOrder(true).build();
    }

    @AfterEach
    void restoreHttpClient() {
        RestUtil.getRestTemplate().setRequestFactory(previousFactory);
    }

    @Test
    void usesConfiguredShopsAndCategoryAndRecordsOnlySuccessfulOrders() throws JobExecutionException {
        when(orders.fetchPlatformOrdersWithShopifyTags(Arrays.asList("AC", "OTHER")))
                .thenReturn(Arrays.asList(order("ok", "Promo, Subscription"), order("rejected", "Priority"),
                        order("partial", "Subscription Recurring Order")));
        expectReview("ok", "标签审核", "200");
        expectReview("rejected", "标签审核", "403");
        when(orders.updateBatchById(argThat(updates -> updates.size() == 1))).thenReturn(true);

        job.execute(context("{\"includedShops\":[\"AC\",\"OTHER\"],\"tags\":[\"Subscription\",\"Priority\"],"
                + "\"abnormalLabelName\":\"标签审核\"}"));

        mabang.verify();
        verify(orders).updateBatchById(argThat(updates -> updates.size() == 1
                && updates.stream().allMatch(order -> "local-ok".equals(order.getId())
                        && "1".equals(order.getAlreadySetAbnormal()) && order.getShopifyTags() == null)));
    }

    @Test
    void usesExistingDefaultsWhenParametersAreOmitted() throws JobExecutionException {
        when(orders.fetchPlatformOrdersWithShopifyTags(Collections.singletonList("AC")))
                .thenReturn(Collections.singletonList(order("subscription", "Subscription")));
        expectReview("subscription", "AC复购订单", "200");
        when(orders.updateBatchById(argThat(updates -> updates.size() == 1))).thenReturn(true);

        job.execute(context("{}"));

        mabang.verify();
        verify(orders).fetchPlatformOrdersWithShopifyTags(Collections.singletonList("AC"));
    }

    @Test
    void emptyShopListSkipsProcessingAndInvalidParametersFailBeforeReadingOrders() throws JobExecutionException {
        job.execute(context("{\"includedShops\":[]}"));
        assertThrows(JobExecutionException.class, () -> job.execute(context("{\"tags\":\"Subscription\"}")));

        verifyNoInteractions(orders);
        mabang.verify();
    }

    private void expectReview(String orderId, String label, String resultCode) {
        mabang.expect(requestTo("https://gwapi.mabangerp.com/api/v2"))
                .andExpect(jsonPath("$.api").value("order-do-order-abnormal"))
                .andExpect(jsonPath("$.data.platformOrderId").value(orderId))
                .andExpect(jsonPath("$.data.abnormal_label_name").value(label))
                .andRespond(withSuccess("{\"code\":\"" + resultCode + "\"}", MediaType.APPLICATION_JSON));
    }

    private static JobExecutionContext context(String parameter) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap data = new JobDataMap();
        data.put("parameter", parameter);
        when(context.getMergedJobDataMap()).thenReturn(data);
        return context;
    }

    private static PlatformOrder order(String id, String tags) {
        PlatformOrder order = new PlatformOrder();
        order.setId("local-" + id);
        order.setPlatformOrderId(id);
        order.setShopifyTags(tags);
        return order;
    }
}
