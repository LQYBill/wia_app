package org.jeecg.modules.business.domain.api.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class WSTrackClient {
    private static final String KEY = "4384C6E6E0946355E063094013AC3D87";
    private static final String SECRET = "-xR8SVqyFgnEXpWKHCFWLpeOC7kRSngv";
    private final String key;
    private final String secret;

    public WSTrackClient() {
        this(KEY, SECRET);
    }

    // Package-private constructor keeps test requests independent of the real credentials.
    WSTrackClient(String key, String secret) {
        this.key = key;
        this.secret = secret;
    }
    @Value("${weishi.customer-id:${WEISHI_CUSTOMER_ID:}}")
    private String customerId;
    @Value("${weishi.track-url:http://120.77.151.112:16832/ws/get/track/info}")
    private String trackUrl;

    public void validateConfiguration() {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(secret)) {
            throw new IllegalStateException("Weishi credentials must not be empty");
        }
    }

    public List<WSTrackResponse.TrackData> query(String billCode) throws Exception {
        validateConfiguration();
        HttpGet request = new HttpGet(new URIBuilder(trackUrl)
                .setParameter("customer_id", StringUtils.hasText(customerId) ? customerId : key)
                .setParameter("order_id", billCode).build());
        request.setHeader("sign", DigestUtils.md5DigestAsHex((key + secret).getBytes(StandardCharsets.UTF_8)));
        request.setConfig(RequestConfig.custom().setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000).setSocketTimeout(30000).build());
        // Never forward the authentication header to a redirected host.
        try (CloseableHttpClient client = HttpClients.custom().disableRedirectHandling().build();
             CloseableHttpResponse response = client.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 200 || response.getEntity() == null) {
                throw new IOException("Weishi track HTTP status " + response.getStatusLine().getStatusCode());
            }
            WSTrackResponse result = new ObjectMapper().readValue(
                    EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), WSTrackResponse.class);
            if (result == null || !Boolean.TRUE.equals(result.getAck())) {
                throw new IOException("Weishi track request rejected (ack=false)");
            }
            return result.getData() == null ? Collections.emptyList() : result.getData();
        }
    }
}

