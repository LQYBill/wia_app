package org.jeecg.modules.business.domain.mabangapi;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.binary.Hex;
import org.jeecg.common.util.RestUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class OrderListRequest {
    private final static String URL = "http://openapi.mabangerp.com";
    private static final HttpMethod METHODE = HttpMethod.POST;
    private static final String devKey = "3763ccb7a9f9c1449a56a00dc013900d";

    public static ResponseEntity<String> toReq(){
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = body();
        String signature = authorization(body);
        headers.add("Authorization", signature);

        return RestUtil.request(URL, METHODE, headers , null, body, String.class);
    }

    private static String authorization(String body) {
        byte[] hmacSha256;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(devKey.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            hmacSha256 = mac.doFinal(body.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hmac-sha256", e);
        }
        return Hex.encodeHexString(hmacSha256);
    }

    private static String body(){
        return "{\n" +
                "  \"developerId\": 100490,\n" +
                "  \"timestamp\": 1617106294,\n" +
                "  \"status\": \"1\",\n" +
                "  \"action\": \"get-order-list\",\n" +
                "  \"paidtimeStart\": \"2021-03-30 00:00:00\",\n" +
                "  \"paidtimeEnd\": \"2021-03-30 23:59:59\"\n" +
                "}";
    }


}
