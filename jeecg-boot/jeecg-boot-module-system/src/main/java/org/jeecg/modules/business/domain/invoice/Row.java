package org.jeecg.modules.business.domain.invoice;

import lombok.Data;

import java.math.BigDecimal;

@Data
class Row {
    private final String description;

    private final BigDecimal PU;

    private final int quantity;

    private final BigDecimal discount;

    private final BigDecimal totalAmount;

    public Row(String description, BigDecimal PU, int quantity, BigDecimal discount, BigDecimal totalAmount) {
        this.description = description;
        this.PU = PU;
        this.quantity = quantity;
        this.discount = discount;
        this.totalAmount = totalAmount;
    }

}
