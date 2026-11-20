package org.jeecg.modules.business.domain.shippingInvoice;

import org.jeecg.modules.business.domain.invoice.Row;
import org.jeecg.modules.business.entity.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EuCustomsHandlingFeeTest {
    private final ShippingInvoiceFactory factory = new ShippingInvoiceFactory();

    private PlatformOrder order(String country, String date) {
        PlatformOrder order = new PlatformOrder();
        order.setCountry(country);
        order.setShippingTime(Date.from(LocalDate.parse(date).atStartOfDay(ZoneId.of("Europe/Paris")).toInstant()));
        return order;
    }

    @Test void chargesFromShippingDateBoundaryUsingDistinctHsCodeCount() {
        LogisticChannel channel = new LogisticChannel();
        channel.setSmallParcelTaxExempted("0");
        assertEquals(BigDecimal.ZERO, factory.calculateEuCustomsHandlingFee(order("France", "2026-10-25"), channel, 2));
        assertEquals(new BigDecimal("4.10"), factory.calculateEuCustomsHandlingFee(order("France", "2026-10-26"), channel, 2));
        assertEquals(new BigDecimal("6.15"), factory.calculateEuCustomsHandlingFee(order("Germany", "2026-10-27"), channel, 3));
    }

    @Test void excludesExemptRoutesAndNonEuParcels() {
        LogisticChannel channel = new LogisticChannel();
        channel.setSmallParcelTaxExempted("1");
        assertEquals(BigDecimal.ZERO, factory.calculateEuCustomsHandlingFee(order("France", "2026-10-26"), channel, 2));
        channel.setSmallParcelTaxExempted("0");
        assertEquals(BigDecimal.ZERO, factory.calculateEuCustomsHandlingFee(order("United Kingdom", "2026-10-26"), channel, 2));
        PlatformOrder unshipped = order("France", "2026-10-26");
        unshipped.setShippingTime(null);
        channel.setSmallParcelTaxExempted("1");
        assertEquals(BigDecimal.ZERO, factory.calculateEuCustomsHandlingFee(unshipped, channel, 2, LocalDate.of(2026, 11, 4)));
        channel.setSmallParcelTaxExempted("0");
        unshipped.setCountry("United Kingdom");
        assertEquals(BigDecimal.ZERO, factory.calculateEuCustomsHandlingFee(unshipped, channel, 2, LocalDate.of(2026, 11, 4)));
    }

    @Test void preShippingUsesInvoiceDateWhenShippingDateIsMissing() {
        PlatformOrder unshipped = order("France", "2026-10-26");
        unshipped.setShippingTime(null);
        LogisticChannel channel = new LogisticChannel();
        channel.setSmallParcelTaxExempted("0");
        assertEquals(BigDecimal.ZERO, factory.calculateEuCustomsHandlingFee(unshipped, channel, 2, LocalDate.of(2026, 10, 25)));
        assertEquals(new BigDecimal("4.10"), factory.calculateEuCustomsHandlingFee(unshipped, channel, 2, LocalDate.of(2026, 10, 26)));
        assertEquals(new BigDecimal("6.15"), factory.calculateEuCustomsHandlingFee(unshipped, channel, 3, LocalDate.of(2026, 11, 4)));
        assertEquals(factory.calculateEuCustomsHandlingFee(unshipped, channel, 2,
                LocalDate.now(ZoneId.of("Europe/Paris"))), factory.calculateEuCustomsHandlingFee(unshipped, channel, 2));
    }

    @Test void actualShippingDateTakesPriorityOverInvoiceDate() {
        LogisticChannel channel = new LogisticChannel();
        channel.setSmallParcelTaxExempted("0");
        assertEquals(BigDecimal.ZERO, factory.calculateEuCustomsHandlingFee(order("France", "2026-10-25"), channel, 2,
                LocalDate.of(2026, 11, 4)));
        assertEquals(new BigDecimal("4.10"), factory.calculateEuCustomsHandlingFee(order("France", "2026-10-26"), channel, 2,
                LocalDate.of(2026, 10, 25)));
    }

    @Test void doesNotInventHsCodesWhenCountIsMissingOrZero() {
        PlatformOrder order = order("France", "2026-10-26");
        assertEquals(BigDecimal.ZERO, factory.calculateEuCustomsHandlingFee(order, null, null));
        assertEquals(BigDecimal.ZERO, factory.calculateEuCustomsHandlingFee(order, null, 0));
        assertEquals(new BigDecimal("2.05"), factory.calculateEuCustomsHandlingFee(order, null, 1));
    }

    private ShippingInvoice invoice(BigDecimal handlingFee) {
        PlatformOrder order = order("France", "2026-10-26");
        order.setPlatformOrderId("parcel-1");
        order.setSmallParcelTax(new BigDecimal("3.05"));
        order.setEuCustomsHandlingFee(handlingFee);
        order.setFretFee(new BigDecimal("10.00"));
        order.setOrderServiceFee(BigDecimal.ZERO);
        order.setPickingFee(BigDecimal.ZERO);
        order.setPackagingMaterialFee(BigDecimal.ZERO);
        return new ShippingInvoice(new Client(), "test", "test",
                Collections.singletonMap(order, Collections.emptyList()), null, null, BigDecimal.ONE);
    }

    @Test void invoiceSeparatesFeeAndIncludesItAlongsideExistingTax() {
        ShippingInvoice invoice = invoice(new BigDecimal("4.10"));
        List<Row<String, Object, Integer, Object, BigDecimal>> rows = invoice.tableData();
        Row<String, Object, Integer, Object, BigDecimal> fee = rows.stream()
                .filter(row -> "Total EU customs handling fee".equals(row.getCol1())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("4.10"), fee.getCol5());
        assertEquals(new BigDecimal("17.15"), invoice.getTotalAmount());
    }

    @Test void historicalInvoicesWithoutHandlingFeeRemainReadable() {
        ShippingInvoice invoice = invoice(null);
        assertTrue(invoice.tableData().stream().noneMatch(row -> "Total EU customs handling fee".equals(row.getCol1())));
        assertEquals(new BigDecimal("13.05"), invoice.getTotalAmount());
    }
}
