package org.jeecg.modules.business.domain.mabangapi.dochangeorder;

import org.jeecg.modules.business.domain.mabangapi.Request;

public class ArchiveOrderRequest extends Request {
    public ArchiveOrderRequest(ArchiveOrderRequestBody body) {
        super(body);
    }

    @Override
    public ChangeOrderResponse send() {
        String jsonString = rawSend().getBody();
        return ChangeOrderResponse.parse(jsonString);
    }
}
