package org.jeecg.modules.business.domain.api.mabang.dochangeorder;

import org.jeecg.modules.business.domain.api.mabang.Request;

public class EditLogisticRequest extends Request {
    public EditLogisticRequest(EditLogisticRequestBody body) {
        super(body);
    }

    @Override
    public ChangeOrderResponse send() {
        String jsonString = rawSend().getBody();
        return ChangeOrderResponse.parse(jsonString);
    }
}
