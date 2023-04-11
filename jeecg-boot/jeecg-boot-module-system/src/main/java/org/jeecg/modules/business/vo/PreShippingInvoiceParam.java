package org.jeecg.modules.business.vo;

import java.util.List;

public class PreShippingInvoiceParam extends AbstractShippingInvoiceParam{

    private static String type = "pre-shipping";
    public PreShippingInvoiceParam(String clientID, List<String> orderIds, String type) {
        super(clientID, orderIds);
        this.type = type;
    }
    public String getType() { return this.type; }

}
