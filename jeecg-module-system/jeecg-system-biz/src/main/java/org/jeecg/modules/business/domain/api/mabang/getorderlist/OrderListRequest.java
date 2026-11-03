package org.jeecg.modules.business.domain.api.mabang.getorderlist;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.mabang.Request;
import org.springframework.http.ResponseEntity;

/**
 * This class contains some key information and necessary procedures
 * to send a request to mabang "get order list" API, for example: target URL,
 * correspondent HTTP method, procedure to generate authorization.
 * <p>
 * One can use static method {@code sendRequest} to send request with body,
 * and then get respective response. Or use instance of this class, see below.
 * <p>
 * Because data returned by target API is paginated. One can retrieve all data
 * by calling next and hasNext.
 */
@Slf4j
public class OrderListRequest extends Request {
    private static final int MAX_RATE_LIMIT_RETRIES = 3;
    private static final long RATE_LIMIT_RETRY_DELAY_MS = 60_000L;

    public OrderListRequest(OrderListRequestBody body) {
        super(body);
    }


    @Override
    public OrderListResponse send() {
        for (int retries = 0; ; retries++) {
            try {
                ResponseEntity<String> res = rawSend();
                return OrderListResponse.parse(JSON.parseObject(res.getBody()));
            } catch (OrderListRequestErrorException e) {
                if (!isRateLimited(e) || retries >= MAX_RATE_LIMIT_RETRIES) {
                    throw e;
                }
                log.warn("Mabang order list rate limit reached; retry {}/{} in 60 seconds.",
                        retries + 1, MAX_RATE_LIMIT_RETRIES);
                try {
                    waitBeforeRetry();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to retry Mabang order list request", interrupted);
                }
            }
        }
    }

    private boolean isRateLimited(OrderListRequestErrorException e) {
        String message = e.getMessage();
        return message != null && "Too Many Attempts".equalsIgnoreCase(
                message.trim().replaceFirst("\\.$", ""));
    }

    protected void waitBeforeRetry() throws InterruptedException {
        Thread.sleep(RATE_LIMIT_RETRY_DELAY_MS);
    }
}
