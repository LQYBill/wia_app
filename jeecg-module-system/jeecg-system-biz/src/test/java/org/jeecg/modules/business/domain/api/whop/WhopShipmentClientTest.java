package org.jeecg.modules.business.domain.api.whop;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WhopShipmentClientTest {
    private final RestTemplate http = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
    private final WhopClient client = new WhopClient("test-key", "2026-11-02-1", http);

    @Test void createsShipmentWithScopedBodyVersionAndIdempotencyHeader() {
        server.expect(requestTo("https://api.whop.com/api/v1/shipments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(header("Api-Version-Date", "2026-11-02-1"))
                .andExpect(header("Idempotency-Key", "stable-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"account_id\":\"biz_test\",\"payment_id\":\"pay_test\",\"tracking_number\":\"TRACK123\"}", true))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":\"ship_test\",\"payment_id\":\"pay_test\",\"account_id\":\"biz_test\",\"tracking_number\":\"TRACK123\"}"));
        assertEquals("ship_test", client.createShipment("biz_test", "pay_test", "TRACK123", "stable-key").getString("id"));
        server.verify();
    }

    @Test void only404MeansShipmentIsMissing() {
        server.expect(requestTo("https://api.whop.com/api/v1/shipments/pay_test"))
                .andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("https://api.whop.com/api/v1/shipments/pay_test"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertNull(client.shipmentForPayment("pay_test"));
        assertThrows(HttpClientErrorException.Forbidden.class, () -> client.shipmentForPayment("pay_test"));
        server.verify();
    }
}
