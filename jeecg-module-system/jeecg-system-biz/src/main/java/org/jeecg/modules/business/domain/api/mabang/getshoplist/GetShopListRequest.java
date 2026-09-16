package org.jeecg.modules.business.domain.api.mabang.getshoplist;

import org.jeecg.modules.business.domain.api.mabang.Request;
import org.jeecg.modules.business.domain.api.mabang.RequestBody;
import org.springframework.http.ResponseEntity;
import java.util.Collections;
import java.util.Map;

public class GetShopListRequest extends Request {
    public GetShopListRequest() {
        super(new RequestBody() {
            public String api() { return "sys-get-shop-list"; }
            public Map<String, Object> parameters() { return Collections.singletonMap("status", 2); }
        });
    }

    @Override
    public GetShopListResponse send() {
        ResponseEntity<String> response = rawSend();
        if (response == null || response.getBody() == null) {
            throw new IllegalStateException("Mabang shop list returned no response");
        }
        return GetShopListResponse.parse(response.getBody());
    }
}