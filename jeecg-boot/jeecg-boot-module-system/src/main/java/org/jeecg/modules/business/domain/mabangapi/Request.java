package org.jeecg.modules.business.domain.mabangapi;


import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.jeecg.common.util.RestUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

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
public abstract class Request {
    private final static String URL = "http://openapi.mabangerp.com";
    private static final HttpMethod METHOD = HttpMethod.POST;
    private static final String DEV_KEY = "3763ccb7a9f9c1449a56a00dc013900d";
    private static final int DEV_ID = 100490;

    private final RequestBody body;

    public Request(RequestBody body) {
        this.body = body;
    }

    /**
     * Sent request to the mabang API with a request body.
     *
     * @return the response of the body or null, if response
     */
    protected ResponseEntity<String> rawSend() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");

        String bodyString = generateJsonBodyString(body);
        String signature = authorization(bodyString);
        headers.add("Authorization", signature);
        return RestUtil.request(URL, METHOD, headers, null, bodyString, String.class);
    }


    public abstract Response send();

    /**
     * By applying algorithm "HmacSHA256", encrypt http body with developer key that provided by mabang.
     *
     * @param body the json string to encrypt
     * @return encrypted string encoded by Hexadecimal.
     */
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

    /**
     * Convert body's json parameters to json string with necessary extra parameter to
     * send request.
     *
     * @param body body to convert
     * @return json string
     */
    private static String generateJsonBodyString(RequestBody body) {
        JSONObject param = body.parameters();
        param.put("developerId", DEV_ID);
        param.put("timestamp", new Date().getTime() / 1000);
        param.put("action", body.action());
        return param.toJSONString();
    }
}
