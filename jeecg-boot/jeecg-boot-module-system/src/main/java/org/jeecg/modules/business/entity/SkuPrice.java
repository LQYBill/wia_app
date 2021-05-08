package org.jeecg.modules.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @Description: The price of a sku
 * @Author: jeecg-boot
 * @Date: 2021-04-16
 * @Version: V1.0
 */
@ApiModel(value = "sku对象", description = "SKU表")
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
@TableName("sku_price")
public class SkuPrice implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id in the DB
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    @Getter
    private String id;

    /**
     * SKU ID
     */
    @Dict(dictTable = "sku", dicText = "erp_code", dicCode = "id")
    @ApiModelProperty(value = "SKU ID")
    @Getter
    private String skuId;

    /**
     * 价格
     */
    @Excel(name = "价格", width = 15)
    @ApiModelProperty(value = "价格")
    @Getter
    private BigDecimal price;

    /**
     * 优惠价起订量
     */
    @Excel(name = "优惠价起订量", width = 15)
    @ApiModelProperty(value = "优惠价起订量")
    @Getter
    private Integer threshold;

    /**
     * 优惠价, maybe null from DB which stands for no discount price
     */
    @Excel(name = "优惠价", width = 15)
    @ApiModelProperty(value = "优惠价")
    @Getter
    private BigDecimal discountedPrice;

    /**
     * 生效日期
     */
    @Excel(name = "生效日期", width = 15, format = "yyyy-MM-dd")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "生效日期")
    @Getter
    private Date date;

    /**
     * The price of a sku depends on its quantity, Given a quantity here, return the correspondent price.
     *
     * @param quantity a quantity
     * @return the price correspondent to the quantity
     */
    public BigDecimal getPrice(int quantity) {
        if (quantity >= threshold) {
            return discountedPrice == null ? price : discountedPrice;
        }
        return price;
    }

    public String toString() {
        return String.format("%s, %s[%d]", price, discountedPrice, threshold);
    }
}
