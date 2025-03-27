package org.jeecg.modules.business.entity.LogisticExpense;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class AnTuExpenseDetail extends AbstractLogisticExpenseDetail {
    @Excel(name="原单号")
    private String platformOrderId;
    @Excel(name="转单号")
    private String trackingNumber;
    @Excel(name="国家")
    private String targetCountry;
    @Excel(name="计费重")
    private BigDecimal chargingWeight;
    @Excel(name="运费")
    private BigDecimal serviceFee;
    @Excel(name="燃油")
    private BigDecimal fuelSurcharge;
    @Excel(name="杂费")
    private BigDecimal additionalFee;
    @Excel(name="总金额")
    private BigDecimal totalFee;
    /**
     * 备注
     * format :  配货：Sku1;Sku2;...;SkuN;
     */
    @Excel(name="备注")
    private String remark;
}
