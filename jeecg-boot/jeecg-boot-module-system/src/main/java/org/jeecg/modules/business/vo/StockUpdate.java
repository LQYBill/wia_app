package org.jeecg.modules.business.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @Description: 库存更新
 * @Author: jeecg-boot
 * @Date: 2021-06-08
 * @Version: V1.0
 */
@Data
@ApiModel(value = "库存更新对象", description = "SKU表")
public class StockUpdate {
    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    private String updateBy;
    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+2", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private Date updateTime;
    /**
     * ERP中商品代码
     */
    @Excel(name = "SKU", width = 15)
    @ApiModelProperty(value = "ERP中商品代码")
    private String erpCode;
    /**
     * 库存数量
     */
    @Excel(name = "库存数量", width = 15)
    @ApiModelProperty(value = "库存数量")
    private Integer availableAmount;
    /**
     * 在途数量
     */
    @Excel(name = "在途数量", width = 15)
    @ApiModelProperty(value = "在途数量")
    private Integer purchasingAmount;
}
