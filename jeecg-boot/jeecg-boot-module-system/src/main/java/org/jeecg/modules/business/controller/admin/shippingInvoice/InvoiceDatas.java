package org.jeecg.modules.business.controller.admin.shippingInvoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class InvoiceDatas {
    private String Id;
    private String invoiceNumber;
    private Integer quantity;
    private BigDecimal discount;
    private BigDecimal finalAmount;
    private BigDecimal paidAmount;
    private List<String> platformOrderIdList;
    private Map<String,BigDecimal> totalPerCountry;
}
