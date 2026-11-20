package org.jeecg.modules.business.domain.api.whop;

import lombok.Value;

@Value
public class WhopShipmentCandidate {
    String paymentId;
    String trackingNumber;
    int trackingCount;
}
