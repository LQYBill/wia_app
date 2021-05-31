package org.jeecg.modules.business.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionDetail {
    private final int count;
    private final BigDecimal unitAmount;
    private final String name;
}
