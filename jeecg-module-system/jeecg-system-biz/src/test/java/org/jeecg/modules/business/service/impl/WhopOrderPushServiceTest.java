package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.business.domain.api.whop.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WhopOrderPushServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final WhopClient client = mock(WhopClient.class);
    private final WhopMabangGateway gateway = mock(WhopMabangGateway.class);
    private final WhopOrderPullService pull = mock(WhopOrderPullService.class);
    private final WhopOrderPushService service = new WhopOrderPushService(jdbc, client,
            new WhopOrderConverter(), gateway, pull);

    @Test void acceptedCreationRemainsSubmittedUntilConfirmed() {
        pending();
        when(gateway.create(any())).thenReturn(JSON.parseObject("{\"code\":200,\"data\":{\"orderId\":\"async-id\"}}"));
        assertEquals(1, service.push("biz_test"));
        verify(jdbc).update(startsWith("update whop_order set push_status=?"), eq("SUBMITTED"),
                anyString(), isNull(), eq("biz_test"), eq("pay_test"));
        verify(pull).save(eq("biz_test"), any());
    }

    @Test void uncertainPreviousSubmissionIsNeverCreatedAgain() {
        when(jdbc.queryForList(anyString(), eq("biz_test")))
                .thenReturn(List.of(Map.of("payment_id", "pay_test", "push_status", "SUBMITTED")));
        when(gateway.check("pay_test")).thenReturn(JSON.parseObject("{\"code\":200,\"data\":{\"isok\":0}}"));
        assertEquals(0, service.push("biz_test"));
        verify(gateway, never()).create(any());
        verifyNoInteractions(client);
    }

    @Test void confirmedRemoteOrderDoesNotGetCreatedAgain() {
        pending();
        when(gateway.check("pay_test")).thenReturn(JSON.parseObject("{\"code\":200,\"data\":{\"isok\":1}}"));
        assertEquals(0, service.push("biz_test"));
        verify(gateway, never()).create(any());
        verify(jdbc).update(startsWith("update whop_order set push_status=?"), eq("SENT"),
                anyString(), isNull(), eq("biz_test"), eq("pay_test"));
    }

    @Test void timeoutKeepsTheDurableSubmissionClaim() {
        pending();
        when(gateway.create(any())).thenThrow(new IllegalStateException("Timeout"));
        assertEquals(0, service.push("biz_test"));
        verify(jdbc).update(startsWith("update whop_order set push_status='SUBMITTED'"),
                anyString(), eq("biz_test"), eq("pay_test"));
        verify(jdbc, never()).update(startsWith("update whop_order set push_status=?"),
                anyString(), anyString(), nullable(String.class), eq("biz_test"), eq("pay_test"));
    }

    private void pending() {
        when(jdbc.queryForList(anyString(), eq("biz_test")))
                .thenReturn(List.of(Map.of("payment_id", "pay_test", "push_status", "PENDING")));
        when(gateway.check("pay_test")).thenReturn(JSON.parseObject("{\"code\":200,\"data\":{\"isok\":0}}"));
        when(client.payment("pay_test")).thenReturn(WhopOrderConverterTest.payment());
        when(jdbc.update(startsWith("update whop_order set push_status='SUBMITTED'"),
                anyString(), eq("biz_test"), eq("pay_test"))).thenReturn(1);
    }
}
