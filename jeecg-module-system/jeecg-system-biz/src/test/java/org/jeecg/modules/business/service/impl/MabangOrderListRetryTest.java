package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.domain.api.mabang.getorderlist.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class MabangOrderListRetryTest {
    private static final String LIMITED = "{\"code\":\"0\",\"message\":\"Too Many Attempts.\"}";
    private static final String SUCCESS = "{\"code\":\"200\",\"message\":\"success\","
            + "\"data\":{\"hasNext\":\"false\",\"nextCursor\":\"next\",\"total\":\"0\",\"data\":[]}}";

    @Test
    void retriesRateLimitedResponseThenReturnsSuccess() {
        StubRequest request = new StubRequest(LIMITED, SUCCESS);
        assertEquals(0, request.send().getDataCount());
        assertEquals(2, request.attempts);
        assertEquals(1, request.waits);
    }

    @Test
    void stopsAfterThreeRetries() {
        StubRequest request = new StubRequest(LIMITED, LIMITED, LIMITED, LIMITED);
        assertThrows(OrderListRequestErrorException.class, request::send);
        assertEquals(4, request.attempts);
        assertEquals(3, request.waits);
    }

    @Test
    void doesNotRetryOtherBusinessErrors() {
        StubRequest request = new StubRequest("{\"code\":\"0\",\"message\":\"Invalid parameters\"}");
        assertEquals("Invalid parameters", assertThrows(OrderListRequestErrorException.class, request::send).getMessage());
        assertEquals(1, request.attempts);
        assertEquals(0, request.waits);
    }

    @Test
    void interruptionStopsRetriesAndRestoresInterruptFlag() {
        StubRequest request = new StubRequest(LIMITED);
        request.interruptWait = true;
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class, request::send);
            assertTrue(error.getCause() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, request.attempts);
        } finally {
            Thread.interrupted();
        }
    }

    private static class StubRequest extends OrderListRequest {
        private final Queue<String> responses;
        private int attempts;
        private int waits;
        private boolean interruptWait;

        StubRequest(String... responses) {
            super(new OrderListRequestBody());
            this.responses = new ArrayDeque<>(Arrays.asList(responses));
        }

        @Override
        protected ResponseEntity<String> rawSend() {
            attempts++;
            return ResponseEntity.ok(responses.remove());
        }

        @Override
        protected void waitBeforeRetry() throws InterruptedException {
            waits++;
            if (interruptWait) {
                throw new InterruptedException("cancelled");
            }
        }
    }
}
