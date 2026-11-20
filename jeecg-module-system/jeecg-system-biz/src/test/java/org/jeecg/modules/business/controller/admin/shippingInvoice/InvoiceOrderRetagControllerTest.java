package org.jeecg.modules.business.controller.admin.shippingInvoice;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.business.entity.PurchaseOrder;
import org.jeecg.modules.business.entity.ShippingInvoice;
import org.jeecg.modules.business.service.IPurchaseOrderService;
import org.jeecg.modules.business.service.IShippingInvoiceService;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.jeecg.modules.business.vo.InvoiceOrdersEditParam;
import org.jeecg.modules.business.vo.ResponsesWithMsg;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvoiceOrderRetagControllerTest {
    private final InvoiceController controller = spy(new InvoiceController());
    private final IPurchaseOrderService purchases = mock(IPurchaseOrderService.class);
    private final IShippingInvoiceService shipping = mock(IShippingInvoiceService.class);
    private final IPlatformOrderMabangService mabang = mock(IPlatformOrderMabangService.class);

    InvoiceOrderRetagControllerTest() {
        ReflectionTestUtils.setField(controller, "purchaseOrderService", purchases);
        ReflectionTestUtils.setField(controller, "iShippingInvoiceService", shipping);
        ReflectionTestUtils.setField(controller, "platformOrderMabangService", mabang);
    }

    @Test
    void manuallyRepairsAllSupportedInvoiceTypesWithoutAutomaticShopOptions() {
        for (String invoice : new String[]{"2026-11-1001", "2026-11-2001", "2026-11-7001"}) {
            activeInvoice(invoice);
            doReturn(Result.OK()).when(controller).checkInvoiceValidity(invoice);
            ResponsesWithMsg<String> response = new ResponsesWithMsg<>();
            when(mabang.editOrdersRemark(invoice)).thenReturn(response);
            Result<?> result = controller.retagOrders(new InvoiceOrdersEditParam(invoice, null));
            assertTrue(result.isSuccess());
            assertSame(response, result.getResult());
            verify(mabang).editOrdersRemark(invoice);
        }
    }

    @Test
    void deniesInvoiceWhenOwnershipCheckFails() {
        String invoice = "2026-11-7001";
        activeInvoice(invoice);
        doReturn(Result.error(403, "Forbidden")).when(controller).checkInvoiceValidity(invoice);
        assertFalse(controller.retagOrders(new InvoiceOrdersEditParam(invoice, null)).isSuccess());
        verifyNoInteractions(mabang);
    }

    @Test
    void rejectsCancelledAndMissingInvoices() {
        String invoice = "2026-11-2001";
        assertFalse(controller.retagOrders(new InvoiceOrdersEditParam(invoice, null)).isSuccess());
        ShippingInvoice cancelled = new ShippingInvoice();
        cancelled.setStatus(0);
        when(shipping.getShippingInvoice(invoice)).thenReturn(cancelled);
        assertFalse(controller.retagOrders(new InvoiceOrdersEditParam(invoice, null)).isSuccess());
        verifyNoInteractions(mabang);
    }

    @Test
    void rejectsInvalidAndCreditInvoiceNumbers() {
        for (String invoice : new String[]{null, "", "bad", "2026-11-8001"}) {
            assertFalse(controller.retagOrders(new InvoiceOrdersEditParam(invoice, null)).isSuccess());
        }
        verifyNoInteractions(purchases, shipping, mabang);
    }

    private void activeInvoice(String invoice) {
        if (invoice.charAt(8) == '1') {
            PurchaseOrder purchase = new PurchaseOrder();
            purchase.setStatus(1);
            when(purchases.getPurchaseByInvoiceNumber(invoice)).thenReturn(purchase);
        } else {
            ShippingInvoice shippingInvoice = new ShippingInvoice();
            shippingInvoice.setStatus(1);
            when(shipping.getShippingInvoice(invoice)).thenReturn(shippingInvoice);
        }
    }
}
