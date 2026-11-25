package org.jeecg.modules.business.domain.api.mabang.dochangeorder;

import org.jeecg.modules.business.domain.api.mabang.Request;
import org.springframework.http.ResponseEntity;

public class ChangeOrderRequest extends Request {
    public ChangeOrderRequest(ChangeOrderRequestBody body) {
        super(body);
    }

    @Override
    public ChangeOrderResponse send() {
        String jsonString = rawSend().getBody();
        return ChangeOrderResponse.parse(jsonString);
    }

    public ChangeOrderResponse sendOnce() {
        ResponseEntity<String> response = rawSend(1);
        if (response == null) {
            throw new IllegalStateException("No response to Mabang order change");
        }
        return ChangeOrderResponse.parse(response.getBody());
    }
}
