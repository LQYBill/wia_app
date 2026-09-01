package org.jeecg.modules.business.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShippingPriceChangeSales {
    private String skuId;
    private String skuCode;
    private String skuName;
    private String imageUrl;
    private String country;
    private String countryCode;
    private String channelName;
    private BigDecimal quantity;
}
