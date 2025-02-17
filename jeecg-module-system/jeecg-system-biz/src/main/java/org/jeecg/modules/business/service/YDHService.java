package org.jeecg.modules.business.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.jeecg.modules.business.domain.api.yd.YDRemoveOrderRequestBody;
import org.jeecg.modules.business.domain.api.yd.YDRequest;
import org.jeecg.modules.business.domain.api.yd.YDTrackingNumberResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YDHService {

    @Value("${ydh.api.apiToken}")
    private String API_TOKEN;
    @Value("${ydh.api.apiKey}")
    private String API_KEY;

    public void deleteYDHTrackingNumbers(List<String> platformOrderNumbers, ExecutorService executor) {
        List<YDRequest> ydRequests = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        platformOrderNumbers.forEach(poNumber -> {
            YDRemoveOrderRequestBody requestBody = new YDRemoveOrderRequestBody(poNumber);
            YDRequest ydRequest = new YDRequest(API_TOKEN, API_KEY, requestBody);
            ydRequests.add(ydRequest);
        });
        List<CompletableFuture<Boolean>> futures = ydRequests.stream()
                .map(request -> CompletableFuture.supplyAsync(() -> {
                    HttpEntity entity = request.send().getEntity();
                    try {
                        String responseString = EntityUtils.toString(entity, "UTF-8");
                        YDTrackingNumberResponse response = mapper.readValue(responseString, YDTrackingNumberResponse.class);
                        if (response.getReturnValue() == 1) {
                            log.info("YDH order {} has been deleted.", request.getYdRequestBody().getServiceMethod());
                            return true;
                        } else {
                            log.error("Failed to delete order {}. Error message: {}", request.getYdRequestBody().getServiceMethod(), response.getCnMessage());
                            return false;
                        }
                    } catch (IOException e) {
                        log.error("Failed to delete order {}. Error message: {}", request.getYdRequestBody().getServiceMethod(), e.getMessage());
                        return false;
                    }
                }, executor))
                .collect(Collectors.toList());
        List<Boolean> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        long nbSuccesses = results.stream().filter(b -> b).count();
        log.info("{}/{} YDH orders have been deleted.", nbSuccesses, platformOrderNumbers.size());
    }
}
