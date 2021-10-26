package org.jeecg.modules.business.vo;

import java.util.List;

public class PreShippingInvoiceParam {

    private final String clientID;

    private final List<String> orderIds;

    public PreShippingInvoiceParam(String clientID, List<String> orderIds) {
        this.clientID = clientID;
        this.orderIds = orderIds;
    }

    public String clientID() {
        return clientID;
    }

    public List<String> orderIds() {
        return orderIds;
    }

    @Override
    public String toString() {
        return "ShippingInvoiceParam{" + clientID +
                ", orderIds=" + orderIds +
                '}';
    }
}
