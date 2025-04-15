package org.jeecg.modules.business.entity.LogisticExpense;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class CaiNiaoExpenseDetail  extends AbstractLogisticExpenseDetail{
    @Excel(name="业务时间")
    private Date date;
    @Excel(name="费用项")
    private String expenseName;
    @Excel(name="运单号")
    private String trackingNumber;
    @Excel(name="物流商品")
    private String logisticChannelName;
    @Excel(name="计费币种")
    private String chargingCurrency;
    @Excel(name="计费金额")
    private BigDecimal billingAmount;
    @Excel(name="支付币种")
    private String paymentCurrency;
    @Excel(name="支付金额")
    private BigDecimal paymentAmount;
    @Excel(name="ERP单号")
    private String platformOrderId;
    @Excel(name="包裹计费重(克)")
    private BigDecimal chargingWeight;
    @Excel(name="外单计泡包裹泡重")
    private BigDecimal volumetricWeight;
    @Excel(name="外单计泡包裹实重")
    private BigDecimal realWeight;
    @Excel(name="收件地址-国家")
    private String targetCountry;
    @Excel(name="目的国家-中文")
    private String targetCountryCn;

    @Getter
    public enum ExpenseType {
        REFUND("货值赔付"),
        SHIPPING_FEE("正向配送费"),
        ADDITIONAL_FEE("分区附加费");

        private final String name;

        ExpenseType(String name) {
            this.name = name;
        }
    }
}
