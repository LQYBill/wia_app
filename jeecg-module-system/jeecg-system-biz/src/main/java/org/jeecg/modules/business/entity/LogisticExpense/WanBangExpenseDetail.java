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
public class WanBangExpenseDetail  extends AbstractLogisticExpenseDetail{
    @Excel(name = "日期", format = "yyyy-MM-dd")
    @JsonFormat(timezone = "GMT+2", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date;
    @Excel(name="转单号")
    private String trackingNumber;
    @Excel(name="渠道名称")
    private String logisticChannelName;
    @Excel(name="计算重(kg)")
    private BigDecimal chargingWeight;
    @Excel(name="目的地")
    private String targetCountry;
    @Excel(name="基本邮费")
    private BigDecimal shippingFee;
    @Excel(name="处理费")
    private BigDecimal registrationFee;
    @Excel(name="关税")
    private BigDecimal customsDuty;
    @Excel(name="偏远附加费")
    private BigDecimal additionalFee;
    @Excel(name="燃油附加费")
    private BigDecimal fuelSurcharge;
    @Excel(name="超尺寸费")
    private BigDecimal oversizeSurcharge;
    @Excel(name="总额")
    private BigDecimal totalFee;
}
