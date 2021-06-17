package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Data container of platform Order, mabang side,
 * annotation is JSON key of mabang.
 * <p>
 * This is a domain object.
 */
@Data
public class Order {
    /**
     * Primary key
     */
    @JSONField(deserialize = false)
    @TableId(type = IdType.ASSIGN_ID)
    private String id = String.valueOf(new DefaultIdentifierGenerator().nextId(this));

    /**
     * Shop name is correspondant the shop erp code in our data base
     */
    @JSONField(name = "shopName")
    private String shopErpCode;

    /**
     * 物流渠道
     */
    @JSONField(name = "myLogisticsChannelName")
    private String logisticChannelName;
    /**
     * 平台订单号码
     */
    @JSONField(name = "platformOrderId")
    private String platformOrderId;

    @JSONField(name = "salesRecordNumber")
    private String platformOrderNumber;

    @JSONField(name = "erpOrderId")
    private String erpOrderId;

    /**
     * If tracking is empty, set it null
     */
    @JSONField(name = "trackNumber")
    private String trackingNumber;

    @JSONField(name = "paidTime", format = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "fr", timezone = "GMT+1")
    private String orderTime;

    @JSONField(name = "expressTime")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "fr", timezone = "GMT+1")
    private String shippingTime;

    /**
     * 订单收件人
     */
    @JSONField(name = "buyerName")
    private String recipient;
    /**
     * 订单收件人国家
     */
    @JSONField(name = "countryNameEN")
    private String country;
    /**
     * 订单收件人邮编
     */
    @JSONField(name = "postCode")
    private String postcode;

    /**
     * 状态
     */
    @JSONField(name = "orderStatus")
    private String status;

    @JSONField(name = "isUnion")
    private String isUnion;

    @JSONField(name = "orderItem")
    private List<OrderItem> orderItems;

    public void setTrackingNumber(String trackingNumber) {
        if (trackingNumber.length() == 0) {
            this.trackingNumber = null;
        } else
            this.trackingNumber = trackingNumber;
    }

    public boolean isUnion() {
        return isUnion.equals("1");
    }

    /**
     * Another order is source only if they have the same recipient, postcode, country.
     *
     * @param candidate candidate
     * @return true if candidate is a merge source, false otherwise
     */
    public boolean isSource(Order candidate) {
        return Objects.equals(recipient, candidate.recipient)
                && Objects.equals(postcode, candidate.postcode)
                && Objects.equals(country, candidate.country);
    }

    public void setShippingTime(String shippingTime) {
        if (shippingTime.length() == 0) {
            this.shippingTime = null;
        } else
            this.shippingTime = shippingTime;
    }
}
