package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.TimeZone;

/**
 * Data container of platform Order, mabang side, annotation is JSON key of mabang
 */
@Data
public class Order {
    /**
     * Primary key
     */
    @JSONField(deserialize = false)
    private String id;

    /**
     * 店铺 erp code
     */
    @JSONField(name = "shopId")
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

    @JSONField(name = "trackNumber")
    private String trackingNumber;

    @JSONField(name = "paidTime")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date orderTime;

    @JSONField(name = "expressTime")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shippingTime;
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
}
