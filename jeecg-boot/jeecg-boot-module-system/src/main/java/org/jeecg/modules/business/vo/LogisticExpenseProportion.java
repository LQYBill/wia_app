package org.jeecg.modules.business.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Proportion of logistic expense per company
 */
@Data
public class LogisticExpenseProportion {

    /**
     * ID of the company
     */
    private final String id;

    /**
     * Name of the company
     */
    private final String name;

    /**
     * expense
     */
    private final BigDecimal expense;
}
