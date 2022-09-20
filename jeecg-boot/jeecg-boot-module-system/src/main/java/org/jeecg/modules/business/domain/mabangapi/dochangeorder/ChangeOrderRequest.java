package org.jeecg.modules.business.domain.mabangapi.dochangeorder;

import org.jeecg.modules.business.domain.mabangapi.Request;
import org.jeecg.modules.business.domain.mabangapi.RequestBody;

public class ChangeOrderRequest extends Request {
    public ChangeOrderRequest(ChangeOrderRequestBody body) {
        super(body);
    }

    @Override
    public ChangeOrderResponse send() {
        String jsonString = rawSend().getBody();
        return ChangeOrderResponse.parse(jsonString);
    }
}
