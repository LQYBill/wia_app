package org.jeecg.modules.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SkuPriceHistory {

    public final static SkuPriceHistory EMPTY = new SkuPriceHistory(null, null, null, null);

    private final String id;

    private final Date effectiveDate;

    private final BigDecimal registrationFee;

    private final BigDecimal shippingFee;
}
