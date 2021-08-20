package org.jeecg.modules.business.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PlatformOrderLogisticExpenseDetail {
    private final String shopErpCode;
    private final String id;
    private final String trackingNumber;
    private final String shop_id;
    private final String logisticChannelName;
    private final String platformOrderId;
    private final String platformOrderNumber;
    private final Date orderTime;
    private final Date shippingTime;
    private final String country;
    private final BigDecimal fretFee;
    private final BigDecimal shippingFee;
    private final BigDecimal vatFee;
    private final String shippingInvoiceNumber;
    private final String logistic_expense_detail_id;
    private final String platform_order_serial_id;
    private final String virtual_tracking_number;
    private final String logistic_internal_number;
    private final BigDecimal real_weight;
    private final BigDecimal volumetric_weight;
    private final BigDecimal charging_weight;
    private final BigDecimal discount;
    private final BigDecimal realShippingFee;
    private final BigDecimal fuel_surcharge;
    private final BigDecimal registration_fee;
    private final BigDecimal second_delivery_fee;
    private final BigDecimal vat;
    private final BigDecimal vat_service_fee;
    private final BigDecimal total_fee;
    private final String logistic_company_id;
    private final BigDecimal additional_fee;
}
