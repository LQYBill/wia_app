package org.jeecg.modules.business.domain.api.whop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

/** Read-only Whop client. Never log credentials or payment response bodies. */
@Component
public class WhopClient {
    private final RestTemplate http;
    private final String apiKey;
    private final String version;

    public WhopClient(@Value("${whop.api-key:${WHOP_API_KEY:}}") String apiKey,
                      @Value("${whop.api-version:2026-11-02-1}") String version) {
        this.apiKey = apiKey;
        this.version = version;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.http = new RestTemplate(factory);
    }

    public JSONObject payments(String accountId, Instant after, Instant before, String cursor) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Configure whop.api-key or WHOP_API_KEY");
        }
        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl("https://api.whop.com/api/v1/payments")
                .queryParam("account_id", accountId).queryParam("first", 100)
                .queryParam("created_after", after.toString()).queryParam("created_before", before.toString());
        if (cursor != null) uri.queryParam("after", cursor);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("Api-Version-Date", version);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        ResponseEntity<String> response = http.exchange(uri.build().encode().toUri(), HttpMethod.GET,
                new HttpEntity<Void>(headers), String.class);
        JSONObject result = JSON.parseObject(response.getBody());
        if (result == null || result.getJSONArray("data") == null || result.getJSONObject("page_info") == null) {
            throw new IllegalStateException("Invalid Whop payments response");
        }
        return result;
    }

    public JSONObject payment(String id) {
        if (id == null || !id.matches("pay_[A-Za-z0-9]+")) throw new IllegalArgumentException("Invalid Whop payment id");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Configure WHOP_API_KEY");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("Api-Version-Date", version);
        JSONObject result = JSON.parseObject(http.exchange(
                "https://api.whop.com/api/v1/payments/" + id, HttpMethod.GET,
                new HttpEntity<Void>(headers), String.class).getBody());
        if (result == null || !id.equals(result.getString("id"))) throw new IllegalStateException("Invalid Whop payment response");
        return result;
    }
}
