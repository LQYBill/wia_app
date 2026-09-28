package org.jeecg.modules.business.service.impl.purchase;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.PurchaseShippingQuoteRecord;
import org.jeecg.modules.business.mapper.PurchaseShippingQuoteMapper;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.jeecg.modules.business.vo.clientPurchaseOrder.PurchaseShippingQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.math.BigDecimal;
import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.entity.PurchaseOrder;
import org.jeecg.modules.business.entity.Promotion;
import org.jeecg.modules.business.mapper.PurchaseOrderMapper;
import org.jeecg.modules.business.mapper.PurchaseOrderContentMapper;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.service.ICurrencyService;
import org.jeecg.modules.business.service.IInvoiceNumberReservationService;
import org.jeecg.modules.business.vo.SkuDetail;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PurchaseShippingSubmissionTest {
    PurchaseOrderServiceImpl service;
    PurchaseShippingQuoteService quotes;
    PurchaseShippingQuoteMapper records;
    PurchaseShippingQuoteRecord record;
    List<SkuQuantity> items = List.of(new SkuQuantity("a", 29));

    @BeforeEach void setup() throws Exception {
        service = new PurchaseOrderServiceImpl(); quotes = mock(PurchaseShippingQuoteService.class);
        records = mock(PurchaseShippingQuoteMapper.class);
        ReflectionTestUtils.setField(service, "shippingQuotes", quotes);
        ReflectionTestUtils.setField(service, "shippingQuoteMapper", records);
        Client client = new Client(); client.setId("client"); client.setCurrency("EUR");
        client.setSmallPurchaseShippingFeeEnabled(true);
        when(quotes.purchaseClient(items)).thenReturn(client);
        when(quotes.validate(eq(client), anyMap())).thenReturn(Map.of("a", "ERP-A"));
        record = new PurchaseShippingQuoteRecord(); record.setId("quote");
        record.setExpiresAt(new Date(System.currentTimeMillis() + 180000));
        when(records.lockQuote("quote", "client")).thenReturn(record);
    }
    @Test void enabledClientCannotSubmitWithoutQuote() {
        assertTrue(assertThrows(UserException.class, () -> service.addQuotedSkuPurchase(items, null, null))
                .getMessage().startsWith("SHIPPING_QUOTE_REQUIRED"));
        verifyNoInteractions(records);
    }
    @Test void consumedQuoteCannotCreateAnotherOrder() {
        record.setPurchaseOrderId("already-created");
        assertTrue(assertThrows(UserException.class, () -> service.addQuotedSkuPurchase(items, null, "quote"))
                .getMessage().startsWith("SHIPPING_QUOTE_USED"));
        verify(records, never()).updateById(any());
    }
    @Test void expiredQuoteRequiresConfirmation() {
        record.setExpiresAt(new Date(0));
        assertTrue(assertThrows(UserException.class, () -> service.addQuotedSkuPurchase(items, null, "quote"))
                .getMessage().startsWith("SHIPPING_QUOTE_EXPIRED"));
    }
    @Test void quantitiesCannotChangeAfterConfirmation() {
        PurchaseShippingQuote quote = new PurchaseShippingQuote();
        quote.setEnabled(true); quote.setCurrency("EUR");
        quote.setQuantities(Map.of("a", 30)); quote.setErpCodes(Map.of("a", "ERP-A"));
        record.setSnapshotJson(JSON.toJSONString(quote));
        assertTrue(assertThrows(UserException.class, () -> service.addQuotedSkuPurchase(items, null, "quote"))
                .getMessage().startsWith("SHIPPING_QUOTE_CHANGED"));
    }
    @Test void anotherClientsQuoteIsRejected() {
        when(records.lockQuote("quote", "client")).thenReturn(null);
        assertTrue(assertThrows(UserException.class, () -> service.addQuotedSkuPurchase(items, null, "quote"))
                .getMessage().startsWith("SHIPPING_QUOTE_INVALID"));
    }

    @Test void acceptedQuotePersistsFeeAndPaymentAmountAndConsumesQuote() throws Exception {
        service = spy(service);
        Client client = quotes.purchaseClient(items);
        IClientService clients = mock(IClientService.class);
        when(clients.getCurrentClient()).thenReturn(client);
        ReflectionTestUtils.setField(service, "clientService", clients);
        IPlatformOrderService platform = mock(IPlatformOrderService.class);
        ReflectionTestUtils.setField(service, "platformOrderService", platform);
        ICurrencyService currencies = mock(ICurrencyService.class);
        ReflectionTestUtils.setField(service, "currencyService", currencies);
        ReflectionTestUtils.setField(service, "invoiceNumberReservationService", mock(IInvoiceNumberReservationService.class));
        ReflectionTestUtils.setField(service, "purchaseOrderMapper", mock(PurchaseOrderMapper.class));
        ReflectionTestUtils.setField(service, "purchaseOrderContentMapper", mock(PurchaseOrderContentMapper.class));
        SkuDetail sku = mock(SkuDetail.class); when(sku.getSkuId()).thenReturn("a");
        when(sku.getPromotion()).thenReturn(Promotion.ZERO_PROMOTION);
        OrderContentDetail detail = mock(OrderContentDetail.class);
        when(detail.getSkuDetail()).thenReturn(sku); when(detail.getQuantity()).thenReturn(29);
        when(detail.totalPrice()).thenReturn(new BigDecimal("10.00"));
        when(detail.reducedAmount()).thenReturn(BigDecimal.ZERO);
        when(platform.searchPurchaseOrderDetail(items)).thenReturn(List.of(detail));
        PurchaseOrder order = new PurchaseOrder(); order.setTotalAmount(new BigDecimal("10.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        doReturn(order).when(service).getById(anyString());
        doReturn(true).when(service).updateById(any(PurchaseOrder.class));
        PurchaseShippingQuote quote = new PurchaseShippingQuote(); quote.setEnabled(true); quote.setCurrency("EUR");
        quote.setQuantities(Map.of("a", 29)); quote.setErpCodes(Map.of("a", "ERP-A"));
        quote.setMerchandiseAmount(new BigDecimal("10.00")); quote.setDiscountAmount(BigDecimal.ZERO);
        quote.setDomesticShippingFee(new BigDecimal("2.00")); quote.setPayableAmount(new BigDecimal("12.00"));
        record.setSnapshotJson(JSON.toJSONString(quote));
        TransactionSynchronizationManager.initSynchronization();
        try {
            String orderId = service.addQuotedSkuPurchase(items, null, "quote");
            assertEquals(orderId, record.getPurchaseOrderId());
            assertEquals(new BigDecimal("2.00"), order.getDomesticShippingFee());
            assertEquals(new BigDecimal("12.00"), order.getFinalAmount());
            verify(service).updateById(order);
            verify(records).updateById(record);
        } finally {
            // Do not execute notification callbacks; this test does not send mail.
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
