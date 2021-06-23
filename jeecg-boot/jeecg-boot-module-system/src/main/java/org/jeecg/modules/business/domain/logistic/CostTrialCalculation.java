package org.jeecg.modules.business.domain.logistic;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.jeecg.modules.business.entity.LogisticChannelPrice;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Data set for shipping cost trial calculation.
 */
@Data
public class CostTrialCalculation {

    private final String logisticsChannelName;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    private final Date effectiveDate;

    private final double unitPrice;

    private final double shippingCost;

    private final double registrationCost;

    private final double additionalCost;


    private CostTrialCalculation(String logisticsChannelName, double unitPrice, double shippingCost,
                                 double registrationCost, double additionalCost, Date effectiveDate) {
        this.logisticsChannelName = logisticsChannelName;
        this.unitPrice = unitPrice;
        this.shippingCost = format(shippingCost);
        this.registrationCost = format(registrationCost);
        this.additionalCost = format(additionalCost);
        this.effectiveDate = effectiveDate;
    }

    public CostTrialCalculation(LogisticChannelPrice price, int weight, String logisticsChannelName) {
        this(logisticsChannelName, price.getCalUnitPrice().doubleValue(), price.calculateShippingCost(weight),
                price.getRegistrationFee().doubleValue(), price.getAdditionalCost().doubleValue(), price.getEffectiveDate());
    }

    @JsonProperty("TotalCost")
    public double getTotalCost() {
        return format(shippingCost + registrationCost + additionalCost);
    }


    /**
     * Format a decimal with 2 unit.
     *
     * @param n the number to format
     * @return the number formatted
     */
    private double format(double n) {
        return BigDecimal.valueOf(n).setScale(2, BigDecimal.ROUND_UP).doubleValue();
    }
}
