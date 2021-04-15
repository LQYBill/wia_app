package org.jeecg.modules.business.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.jeecg.common.aspect.annotation.Dict;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import java.util.Date;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @Description: 平台订单内容
 * @Author: jeecg-boot
 * @Date:   2021-04-08
 * @Version: V1.0
 */
@ApiModel(value="platform_order对象", description="平台订单表")
@Data
@TableName("platform_order_content")
public class PlatformOrderContent implements Serializable {
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
	/**平台订单ID*/
	@Excel(name = "平台订单ID", width = 15, dictTable = "platform_order", dicText = "platform_order_id", dicCode = "id")
    @Dict(dictTable = "platform_order", dicText = "platform_order_id", dicCode = "id")
    @ApiModelProperty(value = "平台订单ID")
    private String platformOrderId;
	/**SKU ID*/
	@Excel(name = "SKU ID", width = 15, dictTable = "sku", dicText = "erp_code", dicCode = "id")
	@Dict(dictTable = "sku", dicText = "erp_code", dicCode = "id")
    @ApiModelProperty(value = "SKU ID")
    private String skuId;
	/**SKU数量*/
	@Excel(name = "SKU数量", width = 15)
    @ApiModelProperty(value = "SKU数量")
    private Integer quantity;
	/**商品采购总费用*/
	@Excel(name = "商品采购总费用", width = 15)
    @ApiModelProperty(value = "商品采购总费用")
    private java.math.BigDecimal purchaseFee;
	/**物流总费用*/
	@Excel(name = "物流总费用", width = 15)
    @ApiModelProperty(value = "物流总费用")
    private java.math.BigDecimal shippingFee;
	/**服务总费用*/
	@Excel(name = "服务总费用", width = 15)
    @ApiModelProperty(value = "服务总费用")
    private java.math.BigDecimal serviceFee;
	/**sku状态*/
    @ApiModelProperty(value = "SKU 状态")
    @Excel(name = "SKU 状态", width = 15, dictTable = "sku_status", dicText = "status_text", dicCode = "status_code")
    @Dict(dictTable = "sku_status", dicText = "status_text", dicCode = "status_code")
    private Integer status;
}
