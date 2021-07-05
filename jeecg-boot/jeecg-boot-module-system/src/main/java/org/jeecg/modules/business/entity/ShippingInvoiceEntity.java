package org.jeecg.modules.business.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Java bean version of shipping invoice, not a domain object.
 */
@Data
@Table("shipping_invoice")
public class ShippingInvoiceEntity {
    @Id
    private String id;

    private final String createBy;

    private final Date createTime;

    private final String updateBy;

    private final Date updateTime;

    private final String invoiceNumber;

    private final BigDecimal totalAmount;

    private final BigDecimal discountAmount;

    private final BigDecimal finalAmount;

    private final BigDecimal paidAmount;

    @PersistenceConstructor
    public ShippingInvoiceEntity(String id,
                                 String createBy,
                                 Date createTime,
                                 String updateBy,
                                 Date updateTime,
                                 String invoiceNumber,
                                 BigDecimal totalAmount,
                                 BigDecimal discountAmount,
                                 BigDecimal finalAmount,
                                 BigDecimal paidAmount) {
        this.id = id;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.invoiceNumber = invoiceNumber;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.paidAmount = paidAmount;
    }

    public static ShippingInvoiceEntity of(
            String invoiceNumber,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal paidAmount
    ) {
        return new ShippingInvoiceEntity(null, "admin", new Date(), "admin", new Date(),
                invoiceNumber, totalAmount, discountAmount, totalAmount.subtract(discountAmount), paidAmount);
    }

    public ShippingInvoiceEntity withId(String id) {
        return new ShippingInvoiceEntity(id, createBy, createTime, updateBy, updateTime, invoiceNumber, totalAmount, discountAmount, finalAmount, paidAmount);
    }
}
