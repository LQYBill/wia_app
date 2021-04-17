package org.jeecg.modules.business.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.math.BigDecimal;
import java.util.Date;

import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @Description: The price of a sku
 * @Author: jeecg-boot
 * @Date: 2021-04-16
 * @Version: V1.0
 */
@ApiModel(value = "sku对象", description = "SKU表")
@Data
@TableName("sku_price")
public class SkuPrice implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id in the DB
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    /**
     * SKU ID
     */
    @Dict(dictTable = "sku", dicText = "erp_code", dicCode = "id")
    @ApiModelProperty(value = "SKU ID")
    private String skuId;

    /**
     * 价格
     */
    @Excel(name = "价格", width = 15)
    @ApiModelProperty(value = "价格")
    private BigDecimal price;

    /**
     * 优惠价起订量
     */
    @Excel(name = "优惠价起订量", width = 15)
    @ApiModelProperty(value = "优惠价起订量")
    private Integer threshold;

    /**
     * 优惠价
     */
    @Excel(name = "优惠价", width = 15)
    @ApiModelProperty(value = "优惠价")
    private BigDecimal discountedPrice;

    /**
     * 生效日期
     */
    @Excel(name = "生效日期", width = 15, format = "yyyy-MM-dd")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "生效日期")
    private Date date;

    /**
     * The price of a sku depends on its quantity, Given a quantity here, return the correspondent price.
     *
     * @param quantity a quantity
     * @return the price correspondent to the quantity
     */
    public BigDecimal getPrice(int quantity) {
        if (quantity > threshold) {
            return discountedPrice;
        }
        return price;
    }
}
