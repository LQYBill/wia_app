package org.jeecg.modules.business.domain.api.ws;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

public class WSTrackClientTest {
    @Test
    void signsRequestDefaultsCustomerToKeyAndHandlesProviderFailures() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> query = new AtomicReference<>();
        AtomicReference<String> sign = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>("{\"ack\":true,\"data\":null}");
        server.createContext("/track", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            sign.set(exchange.getRequestHeaders().getFirst("sign"));
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            WSTrackClient client = new WSTrackClient("abc", "kde");
            set(client, "customerId", "");
            set(client, "trackUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/track");
            assertTrue(client.query("TEST123").isEmpty());
            assertEquals("7a9ffe603ce67eb868bc80c4418b5749", sign.get());
            assertEquals("customer_id=abc&order_id=TEST123", query.get());
            assertFalse(query.get().contains("secret"));
            set(client, "customerId", "customer-2");
            assertTrue(client.query("TEST123").isEmpty());
            assertTrue(query.get().contains("customer_id=customer-2"));
            body.set("{\"ack\":false,\"message\":\"customer missing\"}");
            assertThrows(java.io.IOException.class, () -> client.query("TEST123"));
            body.set("not json");
            assertThrows(java.io.IOException.class, () -> client.query("TEST123"));
        } finally {
            server.stop(0);
        }
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

