package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.api.whop.WhopClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WhopOrderPullServiceTest {
    private final WhopClient client = mock(WhopClient.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final WhopOrderPullService service = new WhopOrderPullService(client, jdbc);
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach void details() {
        when(client.payment(anyString())).thenAnswer(call -> {
            JSONObject payment = JSON.parseObject("{\"account_id\":\"biz_test\",\"currency\":\"eur\","
                    + "\"total\":{\"amount\":\"10.00\",\"currency\":\"eur\"}}");
            payment.put("id", call.getArgument(0));
            return payment;
        });
    }

    @Test void checkpointsOnlyAfterAllPagesAreSaved() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq("biz_test"))).thenReturn(Collections.emptyList());
        when(client.payments(eq("biz_test"), eq(start), any(), isNull()))
                .thenReturn(page("[{\"id\":\"pay_1\",\"status\":\"paid\"}]", true, "cursor1"));
        when(client.payments(eq("biz_test"), eq(start), any(), eq("cursor1")))
                .thenReturn(page("[{\"id\":\"pay_2\",\"substatus\":\"refunded\"}]", false, null));
        assertEquals(2, service.pull("biz_test", start));
        org.mockito.InOrder order = inOrder(jdbc);
        order.verify(jdbc, times(2)).update(startsWith("insert into whop_order "),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        order.verify(jdbc).update(startsWith("insert into whop_sync_state "), eq("biz_test"), anyString());
    }

    @Test void failedPageDoesNotAdvanceCheckpoint() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq("biz_test"))).thenReturn(Collections.emptyList());
        when(client.payments(eq("biz_test"), eq(start), any(), isNull()))
                .thenReturn(page("[{\"id\":\"pay_1\"}]", true, "cursor1"));
        when(client.payments(eq("biz_test"), eq(start), any(), eq("cursor1")))
                .thenThrow(new IllegalStateException("HTTP failure"));
        assertThrows(IllegalStateException.class, () -> service.pull("biz_test", start));
        verify(jdbc, never()).update(startsWith("insert into whop_sync_state "), eq("biz_test"), anyString());
    }

    @Test void resumesWithOverlapAndRejectsRepeatedCursors() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq("biz_test")))
                .thenReturn(Collections.singletonList("2026-10-01T00:00:00Z"));
        Instant overlap = Instant.parse("2026-09-30T23:55:00Z");
        when(client.payments(eq("biz_test"), eq(overlap), any(), nullable(String.class)))
                .thenReturn(page("[]", true, "same"));
        assertThrows(IllegalStateException.class, () -> service.pull("biz_test", null));
        verify(client, times(2)).payments(eq("biz_test"), eq(overlap), any(), nullable(String.class));
        verify(jdbc, never()).update(startsWith("insert into whop_sync_state "), eq("biz_test"), anyString());
    }

    @Test void firstRunRequiresAnExplicitStartAndAccountMustMatch() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq("biz_test"))).thenReturn(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> service.pull("biz_test", null));
        assertThrows(IllegalStateException.class, () -> service.save("biz_test",
                JSON.parseObject("{\"id\":\"pay_1\",\"account_id\":\"biz_other\"}")));
        verifyNoInteractions(client);
    }

    private JSONObject page(String data, boolean next, String cursor) {
        JSONObject result = new JSONObject();
        result.put("data", JSON.parseArray(data));
        JSONObject info = new JSONObject();
        info.put("has_next_page", next);
        info.put("end_cursor", cursor);
        result.put("page_info", info);
        return result;
    }
}
