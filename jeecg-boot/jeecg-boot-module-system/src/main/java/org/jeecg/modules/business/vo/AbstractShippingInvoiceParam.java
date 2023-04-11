package org.jeecg.modules.business.vo;

import java.util.List;

abstract class AbstractShippingInvoiceParam {

    private final String clientID;

    private final List<String> orderIds;


    public AbstractShippingInvoiceParam(String clientID, List<String> orderIds) {
        this.clientID = clientID;
        this.orderIds = orderIds;
    }

    public String clientID() {
        return clientID;
    }

    public List<String> orderIds() {
        return orderIds;
    }

}
