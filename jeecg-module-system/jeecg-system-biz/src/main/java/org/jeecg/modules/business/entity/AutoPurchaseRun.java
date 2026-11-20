package org.jeecg.modules.business.entity;

import lombok.Data;

@Data
public class AutoPurchaseRun {
    private String id;
    private String clientId;
    private String purchaseId;
    private String recipient;
    private String mabangUsername;
    private Boolean autoMarkPaid;
    private Boolean emailSent;
    private String planState;
}
