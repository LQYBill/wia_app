package org.jeecg.modules.business.entity.LogisticExpense;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class CNEExpenseDetail  extends AbstractLogisticExpenseDetail{
    @Excel(name="业务日期")
    private String businessDate;
    @Excel(name="内单号", type = 1)
    private String internalTrackingNumber;
    @Excel(name="转单号")
    private String trackingNumber;
    @Excel(name="参考号")
    private String platformOrderId;
    @Excel(name="产品名称")
    private String logisticChannelName;
    @Excel(name="目的地")
    private String targetCountryCn;
    @Excel(name="计费重（kg）")
    private BigDecimal chargingWeight;
    @Excel(name="金额")
    private BigDecimal totalFee;
    @Excel(name="币种")
    private String currency;
    @Excel(name="备注")
    private String remark;
}
