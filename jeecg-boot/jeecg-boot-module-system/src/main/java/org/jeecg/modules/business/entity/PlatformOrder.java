package org.jeecg.modules.business.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
@ApiModel(value="platform_order对象", description="平台订单表")
@Data
@TableName("platform_order")
public class PlatformOrder implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;
	/**创建人*/
    @ApiModelProperty(value = "创建人")
    private String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private Date createTime;
	/**更新人*/
    @ApiModelProperty(value = "更新人")
    private String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private Date updateTime;
	/**店铺ID*/
	@Excel(name = "店铺ID", width = 15, dictTable = "shop", dicText = "erp_code", dicCode = "id")
    @Dict(dictTable = "shop", dicText = "erp_code", dicCode = "id")
    @ApiModelProperty(value = "店铺ID")
    private String shopId;
	/**物流渠道*/
	@Excel(name = "物流渠道", width = 15, dictTable = "logistic_channel", dicText = "zh_name", dicCode = "zh_name")
    @Dict(dictTable = "logistic_channel", dicText = "zh_name", dicCode = "zh_name")
    @ApiModelProperty(value = "物流渠道")
    private String logisticChannelName;
	/**平台订单号码*/
	@Excel(name = "平台订单号码", width = 15)
    @ApiModelProperty(value = "平台订单号码")
    private String platformOrderId;
	/**平台订单交易号*/
	@Excel(name = "平台订单交易号", width = 15)
    @ApiModelProperty(value = "平台订单交易号")
    private String platformOrderNumber;
	/**物流跟踪号*/
	@Excel(name = "物流跟踪号", width = 15)
    @ApiModelProperty(value = "物流跟踪号")
    private String trackingNumber;
	/**订单交易时间*/
	@Excel(name = "订单交易时间", width = 15, format = "yyyy-MM-dd")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value = "订单交易时间")
    private Date orderTime;
	/**订单发货时间*/
	@Excel(name = "订单发货时间", width = 15, format = "yyyy-MM-dd")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value = "订单发货时间")
    private Date shippingTime;
	/**订单收件人*/
	@Excel(name = "订单收件人", width = 15)
    @ApiModelProperty(value = "订单收件人")
    private String recepient;
	/**订单收件人国家*/
	@Excel(name = "订单收件人国家", width = 15)
    @ApiModelProperty(value = "订单收件人国家")
    private String country;
	/**订单收件人邮编*/
	@Excel(name = "订单收件人邮编", width = 15)
    @ApiModelProperty(value = "订单收件人邮编")
    private String postcode;
	/**物流挂号费*/
	@Excel(name = "物流挂号费", width = 15)
    @ApiModelProperty(value = "物流挂号费")
    private java.math.BigDecimal fretFee;
	/**物流发票号*/
	@Excel(name = "物流发票号", width = 15, dictTable = "shipping_invoice", dicText = "invoice_number", dicCode = "id")
    @Dict(dictTable = "shipping_invoice", dicText = "invoice_number", dicCode = "id")
    @ApiModelProperty(value = "物流发票号")
    private String shippingInvoiceNumber;
	/**状态*/
	@Excel(name = "状态", width = 15)
    @ApiModelProperty(value = "状态")
    private String status;
}
