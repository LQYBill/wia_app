package org.jeecg.modules.business.service.impl.purchase;

import org.jeecg.modules.business.domain.api.mabang.stockGetStockQuantity.*;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.AutoPurchaseMapper;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AutoPurchasePreparationServiceTest {
    private final AutoPurchaseMapper runs = mock(AutoPurchaseMapper.class);
    private final ISkuService skus = mock(ISkuService.class);
    private final IPurchaseOrderService purchases = mock(IPurchaseOrderService.class);
    private final Sku sku = sku();
    private final Client client = client();

    private static Sku sku() {
        Sku result = new Sku(); result.setId("sku"); result.setErpCode("ERP"); return result;
    }
    private static Client client() {
        Client result = new Client(); result.setId("client"); result.setInternalCode("A"); return result;
    }
    private Map<String, SkuStockData> stock(int available, int inbound) {
        WarehouseStock warehouse = new WarehouseStock();
        warehouse.setWarehouseName("SZBA宝安仓");
        warehouse.setStockQuantity(available); warehouse.setShippingQuantity(inbound);
        SkuStockData stock = new SkuStockData(); stock.setWarehouse(Collections.singletonList(warehouse));
        return Collections.singletonMap("ERP", stock);
    }
    private AutoPurchasePreparationService service() {
        return new AutoPurchasePreparationService(runs, skus, purchases) {
            @Override protected Map<String, SkuStockData> loadStock(List<Sku> eligible) { return stock(3, 2); }
        };
    }
    private void setup() throws Exception {
        when(runs.lockClient("client")).thenReturn("client");
        when(runs.eligibleSkus("client")).thenReturn(Collections.singletonList(sku));
        SkuOrderPage row = new SkuOrderPage(); row.setId("sku"); row.setQtyInOrdersNotShipped(12);
        when(skus.getInventoryByClientCode("A")).thenReturn(Collections.singletonList(row));
        when(purchases.addAutomaticPurchase(eq(client), anyList())).thenReturn("purchase");
        PurchaseOrder order = new PurchaseOrder(); order.setInvoiceNumber("PI1");
        when(purchases.getById("purchase")).thenReturn(order);
    }
    @Test void fillsExactShortageAfterAvailableAndInboundStock() {
        List<SkuQuantity> result = AutoPurchasePreparationService.shortages(Collections.singletonList(sku),
                Collections.singletonMap("sku", 12), stock(3, 2));
        assertEquals(7, result.get(0).getQuantity());
        assertTrue(AutoPurchasePreparationService.shortages(Collections.singletonList(sku),
                Collections.singletonMap("sku", 5), stock(3, 2)).isEmpty());
    }
    @Test void refusesIncompleteStockInsteadOfTreatingItAsZero() {
        assertThrows(IllegalStateException.class, () -> AutoPurchasePreparationService.shortages(
                Collections.singletonList(sku), Collections.singletonMap("sku", 12), Collections.emptyMap()));
    }
    @Test void storesInvoiceAndMarksPaidOnlyWhenEnabled() throws Exception {
        setup();
        AutoPurchaseRun result = service().prepare(client, "2026-11-17", "a@example.com", true, "buyer");
        verify(purchases).setPaid(Collections.singletonList("PI1"));
        verify(runs).insert(result, "2026-11-17");
        assertEquals("a@example.com", result.getRecipient());
        assertEquals("purchase", result.getPurchaseId());
    }
    @Test void leavesInvoiceUnpaidWhenDisabled() throws Exception {
        setup(); service().prepare(client, "2026-11-17", "a@example.com", false, "buyer");
        verify(purchases, never()).setPaid(anyList());
    }
    @Test void reusesPendingOrTodaysRunWithoutCreatingAnotherInvoice() throws Exception {
        setup(); AutoPurchaseRun existing = new AutoPurchaseRun();
        when(runs.findBlockingRun("client", "2026-11-17")).thenReturn(existing);
        assertSame(existing, service().prepare(client, "2026-11-17", "changed@example.com", true, "buyer"));
        verifyNoInteractions(purchases, skus);
    }
    @Test void excludesInactiveClientsBeforeAnyPurchaseWork() {
        assertThrows(IllegalArgumentException.class, () -> service().prepare(client, "2026-11-17", "a@example.com", false, "buyer"));
        verifyNoInteractions(purchases, skus);
    }
}
