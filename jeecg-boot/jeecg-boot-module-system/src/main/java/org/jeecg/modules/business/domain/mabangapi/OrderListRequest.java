package org.jeecg.modules.business.domain.mabangapi;

import org.apache.commons.codec.binary.Hex;
import org.jeecg.common.util.RestUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class OrderListRequest {
    private final static String URL = "http://openapi.mabangerp.com";
    private static final HttpMethod METHODE = HttpMethod.POST;
    private static final String DEV_KEY = "3763ccb7a9f9c1449a56a00dc013900d";

    public static ResponseEntity<String> sentRequest(OrderListRequestBody body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");

        String bodyString = body.toJSON().toJSONString();
        String signature = authorization(bodyString);
        headers.add("Authorization", signature);

        return RestUtil.request(URL, METHODE, headers, null, body.toJSON().toJSONString(), String.class);
    }

    private static String authorization(String body) {
        byte[] hmacSha256;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(DEV_KEY.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            hmacSha256 = mac.doFinal(body.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hmac-sha256", e);
        }
        return Hex.encodeHexString(hmacSha256);
    }

}
