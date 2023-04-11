package org.jeecg.modules.business.vo;

import java.util.List;

public class AllShippingInvoiceParam extends AbstractShippingInvoiceParam{
    private static final String type = "all";
    public AllShippingInvoiceParam(String clientID, List<String> orderIds) {
        super(clientID, orderIds);
    }
    public String getType() { return this.type; }
}
