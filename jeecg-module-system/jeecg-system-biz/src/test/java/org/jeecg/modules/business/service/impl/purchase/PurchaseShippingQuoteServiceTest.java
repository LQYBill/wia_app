package org.jeecg.modules.business.service.impl.purchase;

import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.*;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.jeecg.modules.business.vo.clientPurchaseOrder.PurchaseShippingQuote;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PurchaseShippingQuoteServiceTest {
    @Test void combinesSkusAndChargesEachSmallSupplierOnce() {
        List<PurchaseShippingQuote.Group> groups = PurchaseShippingQuoteService.calculateGroups(
                Map.of("a", 10, "b", 19, "c", 30, "d", 1),
                Map.of("a", "A", "b", "A", "c", "B", "d", "C"));
        assertEquals(29, groups.get(0).getQuantity());
        assertEquals(new BigDecimal("2.00"), groups.get(0).getFee());
        assertEquals(new BigDecimal("0.00"), groups.get(1).getFee());
        assertEquals(new BigDecimal("4.00"), groups.stream().map(PurchaseShippingQuote.Group::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    @Test void thirtyCombinedUnitsAreFreeAndLargeQuantitiesDoNotOverflow() {
        assertEquals(0, PurchaseShippingQuoteService.calculateGroups(Map.of("a", 15, "b", 15),
                Map.of("a", "same", "b", "same")).get(0).getFee().signum());
        assertEquals(4294967294L, PurchaseShippingQuoteService.calculateGroups(
                Map.of("a", Integer.MAX_VALUE, "b", Integer.MAX_VALUE),
                Map.of("a", "same", "b", "same")).get(0).getQuantity());
    }
    @Test void rejectsInvalidAndDuplicateQuantities() {
        assertThrows(UserException.class, () -> PurchaseShippingQuoteService.quantities(Collections.emptyList()));
        for (Integer quantity : Arrays.asList(null, 0, -1)) {
            assertThrows(UserException.class, () -> PurchaseShippingQuoteService.quantities(List.of(new SkuQuantity("a", quantity))));
        }
        assertThrows(UserException.class, () -> PurchaseShippingQuoteService.quantities(
                List.of(new SkuQuantity("a", 1), new SkuQuantity("a", 2))));
    }
    @Test void disabledClientDoesNotCallMabang() throws Exception {
        Fixture f = new Fixture(false);
        PurchaseShippingQuote quote = f.service.preview(List.of(new SkuQuantity("a", 2)));
        verifyNoInteractions(f.suppliers);
        assertEquals(0, quote.getDomesticShippingFee().signum());
        assertEquals(new BigDecimal("9.90"), quote.getPayableAmount());
        verify(f.quotes).insert(argThat(r -> "client".equals(r.getClientId()) && r.getSnapshotJson().contains(quote.getQuoteId())));
    }
    @Test void feeIsAddedAfterDiscountBeforeCurrencyConversion() throws Exception {
        Fixture f = new Fixture(true);
        when(f.suppliers.suppliers(anyCollection())).thenReturn(Map.of("ERP-A", "supplier"));
        PurchaseShippingQuote quote = f.service.preview(List.of(new SkuQuantity("a", 2)));
        assertEquals(new BigDecimal("2.00"), quote.getDomesticShippingFee());
        assertEquals(new BigDecimal("12.10"), quote.getPayableAmount());
    }
    @Test void ownershipIsCheckedBeforeSupplierLookup() {
        Fixture f = new Fixture(true);
        when(f.clientSkus.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(UserException.class, () -> f.service.preview(List.of(new SkuQuantity("a", 2))));
        verifyNoInteractions(f.suppliers, f.quotes);
    }
    static class Fixture {
        IClientService clients = mock(IClientService.class);
        ISkuService skus = mock(ISkuService.class);
        ClientSkuMapper clientSkus = mock(ClientSkuMapper.class);
        IPlatformOrderService platformOrders = mock(IPlatformOrderService.class);
        ExchangeRatesMapper rates = mock(ExchangeRatesMapper.class);
        MabangPurchaseSupplierService suppliers = mock(MabangPurchaseSupplierService.class);
        PurchaseShippingQuoteMapper quotes = mock(PurchaseShippingQuoteMapper.class);
        PurchaseOrderMapper purchases = mock(PurchaseOrderMapper.class);
        ISecurityService security = mock(ISecurityService.class);
        PurchaseShippingQuoteService service = new PurchaseShippingQuoteService(clients, skus, clientSkus,
                platformOrders, rates, suppliers, quotes, purchases, security);
        Fixture(boolean enabled) {
            Client client = new Client(); client.setId("client"); client.setCurrency("USD");
            client.setSmallPurchaseShippingFeeEnabled(enabled); when(clients.getCurrentClient()).thenReturn(client);
            ClientSku owned = new ClientSku(); owned.setSkuId("a");
            when(clientSkus.selectList(any())).thenReturn(List.of(owned));
            Sku sku = new Sku(); sku.setId("a"); sku.setErpCode("ERP-A");
            when(skus.listByIds(anyCollection())).thenReturn(List.of(sku));
            when(rates.getLatestExchangeRate("EUR", "USD")).thenReturn(new BigDecimal("1.10"));
            OrderContentDetail detail = mock(OrderContentDetail.class);
            when(detail.getQuantity()).thenReturn(2); when(detail.totalPrice()).thenReturn(new BigDecimal("10.00"));
            when(detail.reducedAmount()).thenReturn(new BigDecimal("1.00"));
            try { when(platformOrders.searchPurchaseOrderDetail(anyList())).thenReturn(List.of(detail)); }
            catch (UserException e) { throw new AssertionError(e); }
        }
    }
}
