package org.jeecg.modules.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @Description: 包裹轨迹
 * @Author: jeecg-boot
 * @Date: 2022-02-18
 * @Version: V1.0
 */
@ApiModel(value = "parcel对象", description = "包裹")
@Data
@TableName("parcel_trace")
public class ParcelTrace implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键*/
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
    /**创建人*/
    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
    /**创建日期*/
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
    /**更新人*/
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
    /**更新日期*/
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
    /**包裹ID*/
    @Excel(name = "包裹ID", width = 15)
    @ApiModelProperty(value = "包裹ID")
    private java.lang.String parcelId;
    /**操作时间*/
    @Excel(name = "操作时间", width = 15, format = "yyyy-MM-dd")
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value = "操作时间")
    private java.util.Date scanTime;
    /**操作类型*/
    @Excel(name = "操作类型", width = 15)
    @ApiModelProperty(value = "操作类型")
    private java.lang.String scanType;
    /**操作描述*/
    @Excel(name = "操作描述", width = 15)
    @ApiModelProperty(value = "操作描述")
    private java.lang.String description;
    /**英文操作描述*/
    @Excel(name = "英文操作描述", width = 15)
    @ApiModelProperty(value = "英文操作描述")
    private java.lang.String descriptionEn;
    /**包裹当前所在地*/
    @Excel(name = "包裹当前所在地", width = 15)
    @ApiModelProperty(value = "包裹当前所在地")
    private java.lang.String traceLocation;
    /**包裹状态*/
    @Excel(name = "包裹状态", width = 15)
    @ApiModelProperty(value = "包裹状态")
    private java.lang.String traceStatus;
    /**轨迹代码*/
    @Excel(name = "轨迹代码", width = 15)
    @ApiModelProperty(value = "轨迹代码")
    private java.lang.String traceCode;
}
