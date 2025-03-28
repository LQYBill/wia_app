package org.jeecg.modules.business.entity.LogisticExpense;

import lombok.Data;

import lombok.EqualsAndHashCode;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class CNERefundDetail extends AbstractLogisticExpenseDetail{
    @Excel(name="转单号")
    private String trackingNumber;
    @Excel(name="退款")
    private BigDecimal totalFee;
    @Excel(name="币种")
    private String currency;
    @Excel(name="退款原因")
    private String remark;
}
