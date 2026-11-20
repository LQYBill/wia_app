package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.jeecg.common.base.BaseMap;
import org.jeecg.common.modules.redis.client.JeecgRedisClient;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RestUtil;
import org.jeecg.modules.business.domain.api.mabang.stockDoChangeStock.SkuChangeRequestErrorException;
import org.jeecg.modules.business.domain.api.mabang.stockDoChangeStock.SkuChangeResponse;
import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.entity.SkuWeight;
import org.jeecg.modules.business.service.ISkuService;
import org.jeecg.modules.business.service.ISkuWeightService;
import org.jeecg.modules.business.vo.ResponsesWithMsg;
import org.jeecg.modules.message.websocket.WebSocketSender;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SkuWeightSyncResultTest {
    private SkuListMabangServiceImpl service;
    private ISkuService skus;
    private ISkuWeightService weights;
    private JeecgRedisClient redis;
    private Object previousRedis;
    private ClientHttpRequestFactory previousFactory;
    private MockRestServiceServer http;

    @BeforeEach
    void setUp() {
        service = new SkuListMabangServiceImpl();
        skus = mock(ISkuService.class);
        weights = mock(ISkuWeightService.class);
        ReflectionTestUtils.setField(service, "skuService", skus);
        ReflectionTestUtils.setField(service, "skuWeightService", weights);
        Subject subject = mock(Subject.class);
        LoginUser user = new LoginUser();
        user.setId("weight-sync-test");
        when(subject.getPrincipal()).thenReturn(user);
        ThreadContext.bind(subject);
        previousRedis = ReflectionTestUtils.getField(WebSocketSender.class, "jeecgRedisClient");
        redis = mock(JeecgRedisClient.class);
        new WebSocketSender().setRedisClient(redis);
        previousFactory = RestUtil.getRestTemplate().getRequestFactory();
        http = MockRestServiceServer.bindTo(RestUtil.getRestTemplate()).build();
    }

    @AfterEach
    void tearDown() {
        ThreadContext.unbindSubject();
        ReflectionTestUtils.setField(WebSocketSender.class, "jeecgRedisClient", previousRedis);
        RestUtil.getRestTemplate().setRequestFactory(previousFactory);
    }

    @Test
    void olderWeightIsSkippedWithoutAnyHttpCallAndReasonIncludesBothDates() {
        when(skus.listByIds(anyCollection())).thenReturn(Collections.singletonList(sku("old")));
        when(weights.getBySkuId("old")).thenReturn(weight("old", "2026-09-10T00:00:00Z"));

        ResponsesWithMsg<String> result = service.mabangSkuWeightUpdate(
                Collections.singletonList(weight("old", "2025-11-15T12:58:28Z")));

        assertTrue(result.getSuccesses().isEmpty());
        assertTrue(result.getFailures().isEmpty());
        String reason = result.getSkipped().get("old-ERP").get(0);
        assertTrue(reason.contains("2025-11-15"));
        assertTrue(reason.contains("2026-09-10"));
        assertTrue(reason.contains("未调用马帮接口"));
        JSONObject data = completion().getJSONObject("data");
        assertEquals(1, data.getIntValue("total"));
        assertEquals(0, data.getIntValue("success"));
        assertEquals(1, data.getIntValue("skip"));
        assertEquals(0, data.getIntValue("failure"));
        assertEquals(reason, data.getJSONObject("skipped").getJSONArray("old-ERP").getString(0));
        http.verify();
    }

    @Test
    void mixedBatchCountsOnlyActualUpdatesAsSuccessAndPreservesFailureReason() {
        when(skus.listByIds(anyCollection())).thenReturn(Arrays.asList(sku("old"), sku("ok"), sku("rejected")));
        when(weights.getBySkuId("old")).thenReturn(weight("old", "2026-11-17T00:00:00Z"));
        when(weights.getBySkuId("ok")).thenReturn(weight("ok", "2026-11-16T00:00:00Z"));
        when(weights.getBySkuId("rejected")).thenReturn(weight("rejected", "2026-11-16T00:00:00Z"));
        http.expect(requestTo("https://gwapi.mabangerp.com/api/v2"))
                .andRespond(withSuccess("{\"code\":\"200\",\"data\":{\"nextCursor\":\"\",\"data\":[]}}", MediaType.APPLICATION_JSON));
        http.expect(jsonPath("$.data.stockSku").value("ok-ERP"))
                .andRespond(withSuccess("{\"code\":\"200\",\"data\":{\"stockId\":123}}", MediaType.APPLICATION_JSON));
        http.expect(jsonPath("$.data.stockSku").value("rejected-ERP"))
                .andRespond(withSuccess("{\"code\":\"403\",\"message\":\"没有更新权限\"}", MediaType.APPLICATION_JSON));

        ResponsesWithMsg<String> result = service.mabangSkuWeightUpdate(Arrays.asList(
                weight("old", "2025-11-15T00:00:00Z"), weight("ok", "2026-11-17T00:00:00Z"),
                weight("rejected", "2026-11-17T00:00:00Z"), weight("missing", "2026-11-17T00:00:00Z")));

        assertEquals(Collections.singleton("ok-ERP"), result.getSuccesses().keySet());
        assertEquals(Collections.singleton("old-ERP"), result.getSkipped().keySet());
        assertEquals(2, result.getFailures().size());
        assertTrue(result.getFailures().get("rejected-ERP").get(0).contains("没有更新权限"));
        JSONObject done = completion();
        JSONObject data = done.getJSONObject("data");
        assertEquals(4, data.getIntValue("total"));
        assertEquals(1, data.getIntValue("success"));
        assertEquals(1, data.getIntValue("skip"));
        assertEquals(2, data.getIntValue("failure"));
        assertEquals("马帮重量同步处理完成：同步成功 1，跳过 1，失败 2", done.getString("msgTxt"));
        http.verify();
    }

    @Test
    void successCodeWithoutResultIsFailureRatherThanNullPointer() {
        assertThrows(SkuChangeRequestErrorException.class,
                () -> SkuChangeResponse.parse(JSON.parseObject("{\"code\":\"200\"}"), "sku"));
    }

    private JSONObject completion() {
        ArgumentCaptor<BaseMap> messages = ArgumentCaptor.forClass(BaseMap.class);
        verify(redis, atLeastOnce()).sendMessage(anyString(), messages.capture());
        return messages.getAllValues().stream()
                .map(message -> JSON.parseObject((String) message.get("message")))
                .filter(message -> "complete".equals(message.getString("type")))
                .findFirst().orElseThrow(() -> new AssertionError("Missing completion message"));
    }

    private static Sku sku(String id) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setErpCode(id + "-ERP");
        return sku;
    }

    private static SkuWeight weight(String id, String date) {
        SkuWeight weight = new SkuWeight();
        weight.setSkuId(id);
        weight.setWeight(100);
        weight.setEffectiveDate(Date.from(Instant.parse(date)));
        return weight;
    }
}
