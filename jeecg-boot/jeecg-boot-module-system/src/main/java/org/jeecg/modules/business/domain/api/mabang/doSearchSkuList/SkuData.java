package org.jeecg.modules.business.domain.api.mabang.doSearchSkuList;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sku")
public class SkuData {
    /**
     * Primary key
     */
    @JSONField(deserialize = false)
    @TableId(type = IdType.ASSIGN_ID)
    private String id = IdWorker.getIdStr();

    @JSONField(name="id")
    private Integer stockSkuId;
    @JSONField(name="stockSku")
    private String erpCode;
    @JSONField(name="nameCN")
    private String nameCN;
    @JSONField(name="nameEN")
    private String nameEN;
    /**
     * 1.自动创建; 2.待开发; 3.正常; 4.清仓; 5.停止销售
     */
    @JSONField(name="status")
    private Integer status;
    @JSONField(name="originalSku")
    private String originalSku;
    @JSONField(name="salePrice")
    private BigDecimal salePrice;
    @JSONField(name="declareValue")
    private BigDecimal declareValue;
    /**
     * if stockPicture is empty, we use it
     */
    @JSONField(name="stockPicture")
    private String stockPicture;
    /**
     * else we use salePicture
     */
    @JSONField(name="salePicture")
    private String salePicture;
    /**
     *  length, width and height are used in computing volume of item, but volume is now an obsolete field
     */
    @JSONField(name="length")
    private String length;
    @JSONField(name="width")
    private String width;
    @JSONField(name="height")
    private String height;
    @JSONField(name="weight")
    private double weight;
    /**
     * saleRemark contains the weight
     */
    @JSONField(name="saleRemark")
    private String saleRemark;
    /**
     * 是否含电池:1是;2否
     */
    @JSONField(name="hasBattery")
    private Integer hasBattery;
    /**
     * 带磁:1:是;2:否
     */
    @JSONField(name="magnetic")
    private Integer magnetic ;

    public SkuStatus getStatus() {
        return SkuStatus.fromCode(this.status);
    }
    public String toString() {
        return "ID : " + this.id +
                "\nStockSkuId : " + this.stockSkuId +
                "\nStockSku : " + this.erpCode +
                "\nStatus : " + this.status +
                "\nEn name : " + this.nameEN +
                "\nZh name : " + this.nameCN +
                "\nDeclared Value : " + this.declareValue +
                "\nSale Price : " + this.salePrice +
                "\nSale Remark (weight): " + this.saleRemark +
                "\nStock Picture : " + this.stockPicture +
                "\nsale Picture : " + this.salePicture +
                "\nBattery : " + this.hasBattery +
                "\nMagnetic : " + this.magnetic
                ;
    }
}
