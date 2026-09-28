package org.jeecg.modules.business.service.impl.purchase;

import org.jeecg.modules.business.domain.invoice.Row;
import org.jeecg.modules.business.domain.purchase.invoice.PurchaseInvoice;
import org.jeecg.modules.business.domain.purchase.invoice.PurchaseInvoiceEntry;
import org.jeecg.modules.business.domain.shippingInvoice.CompleteInvoice;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.vo.SkuDetail;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseInvoiceShippingTest {
    static class Invoice extends PurchaseInvoice {
        Invoice() {
            super(new Client(), "test", "test", List.of(new PurchaseInvoiceEntry("a", "Product", 2,
                    new BigDecimal("10.00"))), Collections.emptyList(), BigDecimal.ONE);
        }
        List<Row<String, BigDecimal, Integer, BigDecimal, BigDecimal>> rows() { return tableData(); }
    }
    static class Complete extends CompleteInvoice {
        Complete() {
            super(new Client(), "test", "test", Collections.emptyMap(), null, null,
                    List.of(new PurchaseInvoiceEntry("a", "Product", 2, new BigDecimal("10.00"))),
                    Collections.emptyList(), BigDecimal.ONE);
        }
        List<Row<String, Object, Integer, Object, BigDecimal>> rows() { return tableData(); }
    }
    @Test void shippingIsSeparateFromProductAndNotAddedForLegacyOrders() {
        Invoice invoice = new Invoice();
        assertEquals(1, invoice.rows().size());
        invoice.setDomesticShippingFee(new BigDecimal("4.00"));
        assertEquals(2, invoice.rows().size());
        assertEquals("China domestic shipping fee", invoice.rows().get(1).getCol1());
        assertEquals(new BigDecimal("4.00"), invoice.rows().get(1).getCol5());
        assertNull(invoice.rows().get(1).getCol4());
        assertEquals(new BigDecimal("10.00"), invoice.rows().get(0).getCol5());
    }
    @Test void existingWaiverFeeIsCountedOnlyOnce() {
        OrderContentDetail detail = mock(OrderContentDetail.class);
        SkuDetail sku = mock(SkuDetail.class); when(sku.getSkuId()).thenReturn("a");
        when(detail.getSkuDetail()).thenReturn(sku); when(detail.getQuantity()).thenReturn(1);
        when(detail.totalPrice()).thenReturn(new BigDecimal("10.00"));
        when(detail.reducedAmount()).thenReturn(new BigDecimal("1.00"));
        ShippingFeesWaiver waiver = new ShippingFeesWaiver(); waiver.setId("waiver");
        waiver.setThreshold(30); waiver.setFees(new BigDecimal("2.00"));
        OrdersStatisticData totals = OrdersStatisticData.makeData(List.of(detail), Map.of(waiver, List.of("a")));
        assertEquals(new BigDecimal("11.00"), totals.finalAmount());
    }
    @Test void completeInvoiceIncludesDomesticShippingFeeAsSeparateRow() {
        Complete invoice = new Complete();
        invoice.setDomesticShippingFee(new BigDecimal("2.00"));
        assertEquals("China domestic shipping fee", invoice.rows().get(1).getCol1());
        assertEquals(new BigDecimal("2.00"), invoice.rows().get(1).getCol5());
        assertNull(invoice.rows().get(1).getCol4());
    }
}
