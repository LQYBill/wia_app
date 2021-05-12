package org.jeecg.modules.business.vo.inventory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @Description: SKU表
 * @Author: jeecg-boot
 * @Date: 2021-05-08
 * @Version: V1.0
 */
@ApiModel(value = "Inventory Record", description = "Entry in client inventory page")
@Data
@TableName("inventory_record")
public class InventoryRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    /**
     * 商品ID
     */
    @Excel(name = "商品ID", width = 15, dictTable = "product", dicText = "code", dicCode = "id")
    @Dict(dictTable = "product", dicText = "en_name", dicCode = "id")
    @ApiModelProperty(value = "商品ID")
    private String productId;
    /**
     * ERP中商品代码
     */
    @Excel(name = "ERP中商品代码", width = 15)
    @ApiModelProperty(value = "ERP中商品代码")
    private String erpCode;
    /**
     * 库存数量
     */
    @Excel(name = "库存数量", width = 15)
    @ApiModelProperty(value = "库存数量")
    private Integer availableAmount;

    @Excel(name = "green quantity", width = 15)
    @ApiModelProperty(value = "greenQuantity")
    private Integer greenQuantity;

    @Excel(name = "redQuantity", width = 15)
    @ApiModelProperty(value = "redQuantity")
    private Integer redQuantity;

    /**
     * 图片链接
     */
    @Excel(name = "图片链接", width = 15)
    @ApiModelProperty(value = "图片链接")
    private String imageSource;
}
