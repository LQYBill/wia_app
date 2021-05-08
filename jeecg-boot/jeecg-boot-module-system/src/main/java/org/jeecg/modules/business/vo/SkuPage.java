package org.jeecg.modules.business.vo;

import java.util.List;
import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.entity.SkuPrice;
import org.jeecg.modules.business.entity.ShippingDiscount;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecgframework.poi.excel.annotation.ExcelEntity;
import org.jeecgframework.poi.excel.annotation.ExcelCollection;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @Description: SKU表
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
@Data
@ApiModel(value="skuPage对象", description="SKU表")
public class SkuPage {
	/**主键*/
	@ApiModelProperty(value = "主键")
	private java.lang.String id;
	/**创建人*/
	@ApiModelProperty(value = "创建人")
	private java.lang.String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
	@DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value = "创建日期")
	private java.util.Date createTime;
	/**更新人*/
	@ApiModelProperty(value = "更新人")
	private java.lang.String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
	@DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value = "更新日期")
	private java.util.Date updateTime;
	/**商品ID*/
	@Excel(name = "商品ID", width = 15, dictTable = "product", dicText = "code", dicCode = "id")
	@Dict(dictTable = "product", dicText = "code", dicCode = "id")
	@ApiModelProperty(value = "商品ID")
	private java.lang.String productId;
	/**ERP中商品代码*/
	@Excel(name = "ERP中商品代码", width = 15)
	@ApiModelProperty(value = "ERP中商品代码")
	private java.lang.String erpCode;
	/**库存数量*/
	@Excel(name = "库存数量", width = 15)
	@ApiModelProperty(value = "库存数量")
	private java.lang.Integer availableAmount;
	/**在途数量*/
	@Excel(name = "在途数量", width = 15)
	@ApiModelProperty(value = "在途数量")
	private java.lang.Integer purchasingAmount;
	/**图片链接*/
	@Excel(name = "图片链接", width = 15)
	@ApiModelProperty(value = "图片链接")
	private java.lang.String imageSource;

	@ExcelCollection(name="SKU价格表")
	@ApiModelProperty(value = "SKU价格表")
	private List<SkuPrice> skuPriceList;
	@ExcelCollection(name="SKU物流折扣")
	@ApiModelProperty(value = "SKU物流折扣")
	private List<ShippingDiscount> shippingDiscountList;


}
