package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.jeecg.common.util.RestUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
public class OrderListRequest {
    private final static String URL = "http://openapi.mabangerp.com";
    private static final HttpMethod METHOD = HttpMethod.POST;
    private static final String DEV_KEY = "3763ccb7a9f9c1449a56a00dc013900d";

    /**
     * Instance's current request.
     */
    private final OrderListRequestBody currentBody;
    /**
     * Response of last request.
     */
    private OrderListResponse currentResponse;

    private List<Order> currentOrders;

    private int currentIndex;

    public OrderListRequest(OrderListRequestBody firstBody) {
        this.currentBody = firstBody;
        this.currentResponse = null;
        currentOrders = null;
        this.currentIndex = 0;
    }

    /**
     * Check whether there are still order left.
     *
     * @return true if there are, otherwise false.
     */
    public boolean hasNext() throws OrderListRequestErrorException {
        // if never sent request, send the request and check result length
        if (currentResponse == null) {
            log.trace("Current response is null");
            this.currentResponse = sendRequest(currentBody);
            currentOrders = currentResponse.getData().toJavaList(Order.class);
            currentIndex = 0;
            log.trace("Response size: {}", currentResponse.getDataCount());
            log.info("Current page: {}", currentBody.getPage());
            return currentOrders.size() != 0;
        }
        // current index doesn't arrive at the end, return true.
        if (currentIndex < currentOrders.size()) {
            log.trace("Current index: {}, total size: {}", currentIndex, currentOrders.size());
            return true;
        }
        // although at the end, but still has page left
        if (currentBody.getPage() < currentResponse.getTotalPage()) {
            log.trace("Current page: {}, total size: {}", currentBody.getPage(), currentResponse.getTotalPage());
            currentBody.goNextPage();
            this.currentResponse = sendRequest(currentBody);
            currentOrders = currentResponse.getData().toJavaList(Order.class);
            currentIndex = 0;
            return true;
        }
        // at end and no page left
        return false;
    }

    /**
     * Get next Order.
     *
     * @return next order.
     * @throws NoSuchElementException         if data is already empty.
     * @throws OrderListRequestErrorException if request format is not valid.
     */
    public Order next() throws OrderListRequestErrorException {
        if (!hasNext())
            throw new NoSuchElementException();
        currentIndex++;
        return currentOrders.get(currentIndex - 1);
    }

    /**
     * Get all the orders left.
     *
     * @return all orders left
     * @throws OrderListRequestErrorException in case of error of request body
     */
    public List<Order> getAll() throws OrderListRequestErrorException {
        List<Order> res = new ArrayList<>();
        while (hasNext()) {
            res.add(next());
        }
        return res;
    }

    /**
     * Sent request to the mabang API with a request body.
     *
     * @param body the body to carry
     * @return the response of the body or null, if response
     * @throws OrderListRequestErrorException if response represents error.
     */
    public static OrderListResponse sendRequest(OrderListRequestBody body) throws OrderListRequestErrorException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");

        String bodyString = body.toJSON().toJSONString();
        String signature = authorization(bodyString);
        headers.add("Authorization", signature);

        ResponseEntity<String> res = RestUtil.request(URL, METHOD, headers, null, body.toJSON().toJSONString(), String.class);;
        return OrderListResponse.parse(JSON.parseObject(res.getBody()));
    }

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
}
