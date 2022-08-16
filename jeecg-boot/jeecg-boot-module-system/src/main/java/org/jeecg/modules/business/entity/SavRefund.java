package org.jeecg.modules.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @Description: 售后退款
 * @Author: jeecg-boot
 * @Date: 2022-08-12
 * @Version: V1.0
 */
@Data
@TableName("sav_refund")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "sav_refund对象", description = "售后退款")
public class SavRefund implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+2", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+2", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
    /**
     * 平台订单ID
     */
    @Excel(name = "平台订单ID", width = 15, dictTable = "platform_order", dicText = "platform_order_id", dicCode = "id")
    @Dict(dictTable = "platform_order", dicText = "platform_order_id", dicCode = "id")
    @ApiModelProperty(value = "平台订单ID")
    private java.lang.String platformOrderId;
    /**
     * 采购退款
     */
    @Excel(name = "采购退款", width = 15)
    @ApiModelProperty(value = "采购退款")
    private java.lang.String purchaseRefund;
    /**
     * 采购退款金额
     */
    @Excel(name = "采购退款金额", width = 15)
    @ApiModelProperty(value = "采购退款金额")
    private java.math.BigDecimal purchaseRefundAmount;
    /**
     * 运费退款
     */
    @Excel(name = "运费退款", width = 15)
    @ApiModelProperty(value = "运费退款")
    private java.lang.String shippingRefund;
    /**
     * 退款发票ID
     */
    @Excel(name = "退款发票ID", width = 15, dictTable = "shipping_invoice", dicText = "invoice_number", dicCode = "id")
    @Dict(dictTable = "shipping_invoice", dicText = "invoice_number", dicCode = "id")
    @ApiModelProperty(value = "退款发票ID")
    private java.lang.String invoiceId;
    /**
     * 退款日期
     */
    @Excel(name = "退款日期", width = 15, format = "yyyy-MM-dd")
    @JsonFormat(timezone = "GMT+2", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "退款日期")
    private java.util.Date refundDate;
}
