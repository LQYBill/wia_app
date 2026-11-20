package org.jeecg.modules.business.domain.shippingInvoice;

import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.LogisticChannel;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmallParcelTaxTest {
    private final ShippingInvoiceFactory factory = new ShippingInvoiceFactory();

    @Test
    void taxableEuParcelsUseDistinctHsCodesWithExistingMinimumCharge() {
        PlatformOrder order = new PlatformOrder();
        order.setCountry("France");
        LogisticChannel channel = new LogisticChannel();
        channel.setSmallParcelTaxExempted("0");
        assertEquals(new BigDecimal("6.10"), factory.calculateSmallParcelTax(order, channel, 2));
        assertEquals(new BigDecimal("3.05"), factory.calculateSmallParcelTax(order, channel, 0));
        assertEquals(new BigDecimal("3.05"), factory.calculateSmallParcelTax(order, channel, null));
    }

    @Test
    void exemptAndNonEuParcelsHaveZeroTax() {
        PlatformOrder order = new PlatformOrder();
        order.setCountry("France");
        LogisticChannel channel = new LogisticChannel();
        channel.setSmallParcelTaxExempted("1");
        assertEquals(BigDecimal.ZERO, factory.calculateSmallParcelTax(order, channel, 2));
        channel.setSmallParcelTaxExempted("0");
        order.setCountry("United Kingdom");
        assertEquals(BigDecimal.ZERO, factory.calculateSmallParcelTax(order, channel, 2));
    }

    @Test
    void historicalNullTaxDoesNotBreakInvoiceTotals() {
        PlatformOrder order = new PlatformOrder();
        order.setCountry("France");
        order.setPlatformOrderId("parcel-1");
        order.setFretFee(new BigDecimal("10.00"));
        order.setOrderServiceFee(BigDecimal.ZERO);
        order.setPickingFee(BigDecimal.ZERO);
        order.setPackagingMaterialFee(BigDecimal.ZERO);
        ShippingInvoice invoice = new ShippingInvoice(new Client(), "test", "test",
                Collections.singletonMap(order, Collections.emptyList()), null, null, BigDecimal.ONE);

        assertEquals(BigDecimal.ZERO, invoice.tableData().stream()
                .filter(row -> "Total small parcel tax".equals(row.getCol1()))
                .findFirst().orElseThrow(AssertionError::new).getCol5());
        assertEquals(new BigDecimal("10.00"), invoice.getTotalAmount());
    }
}
