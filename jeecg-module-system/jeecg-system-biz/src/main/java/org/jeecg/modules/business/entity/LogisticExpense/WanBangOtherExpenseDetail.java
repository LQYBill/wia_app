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
public class WanBangOtherExpenseDetail  extends AbstractLogisticExpenseDetail{
    @Excel(name="时间", format = "yyyy-MM-dd")
    @JsonFormat(timezone = "GMT+2", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date;
    @Excel(name="类型")
    private String additionalFeeType;
    @Excel(name="金额")
    private BigDecimal totalFee;
    @Excel(name="快递单号")
    private String trackingNumber;
    @Excel(name="重量(Kg)")
    private BigDecimal weight;
    @Excel(name="备注")
    private String remark;
}
