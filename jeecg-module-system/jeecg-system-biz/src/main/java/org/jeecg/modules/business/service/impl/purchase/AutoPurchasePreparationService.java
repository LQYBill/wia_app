package org.jeecg.modules.business.service.impl.purchase;

import org.jeecg.modules.business.domain.api.mabang.stockGetStockQuantity.*;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.AutoPurchaseMapper;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.SkuOrderPage;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/** Atomically reserves a client's daily invoice before any email or purchase-plan side effects. */
@Service
public class AutoPurchasePreparationService {
    private final AutoPurchaseMapper runs;
    private final ISkuService skus;
    private final IPurchaseOrderService purchases;

    public AutoPurchasePreparationService(AutoPurchaseMapper runs, ISkuService skus, IPurchaseOrderService purchases) {
        this.runs = runs; this.skus = skus; this.purchases = purchases;
    }

    @Transactional(rollbackFor = Exception.class)
    public AutoPurchaseRun prepare(Client client, String day, String recipient, boolean markPaid, String mabangUsername) throws Exception {
        if (runs.lockClient(client.getId()) == null) throw new IllegalArgumentException("Client is not active: " + client.getInternalCode());
        AutoPurchaseRun existing = runs.findBlockingRun(client.getId(), day);
        if (existing != null) return existing;
        List<Sku> eligible = runs.eligibleSkus(client.getId());
        if (eligible.isEmpty()) return null;
        Map<String, Integer> demand = skus.getInventoryByClientCode(client.getInternalCode()).stream()
                .collect(Collectors.toMap(SkuOrderPage::getId, row -> row.getQtyInOrdersNotShipped() == null ? 0 : row.getQtyInOrdersNotShipped()));
        List<SkuQuantity> items = shortages(eligible, demand, loadStock(eligible));
        if (items.isEmpty()) return null;
        AutoPurchaseRun run = new AutoPurchaseRun();
        run.setId(UUID.randomUUID().toString());
        run.setClientId(client.getId());
        run.setRecipient(recipient);
        run.setMabangUsername(mabangUsername);
        run.setAutoMarkPaid(markPaid);
        run.setPurchaseId(purchases.addAutomaticPurchase(client, items));
        run.setEmailSent(false);
        run.setPlanState("READY");
        if (markPaid) purchases.setPaid(Collections.singletonList(purchases.getById(run.getPurchaseId()).getInvoiceNumber()));
        runs.insert(run, day);
        return run;
    }

    /** Read live stock: a stale local snapshot must not cause another day's duplicate purchase. */
    protected Map<String, SkuStockData> loadStock(List<Sku> eligible) {
        Map<String, SkuStockData> result = new HashMap<>();
        List<String> codes = eligible.stream().map(Sku::getErpCode).collect(Collectors.toList());
        if (codes.stream().anyMatch(c -> c == null || c.trim().isEmpty()) || new HashSet<>(codes).size() != codes.size()) {
            throw new IllegalArgumentException("Missing or duplicate ERP codes in automatic purchase SKUs");
        }
        for (int offset = 0; offset < codes.size(); offset += 100) {
            List<String> batch = codes.subList(offset, Math.min(offset + 100, codes.size()));
            SkuStockRequestBody body = new SkuStockRequestBody().setStockSkus(batch).setTotal(batch.size());
            for (SkuStockData stock : new SkuStockStream(new SkuStockRawStream(body)).all()) {
                result.put(stock.getStockSku(), stock);
            }
        }
        return result;
    }

    static List<SkuQuantity> shortages(List<Sku> eligible, Map<String, Integer> demand, Map<String, SkuStockData> live) {
        List<SkuQuantity> items = new ArrayList<>();
        for (Sku sku : eligible) {
            SkuStockData stock = live.get(sku.getErpCode());
            if (stock == null || stock.getWarehouse() == null || stock.getWarehouseStock("SZBA宝安仓") == null
                    || stock.getWarehouseStock("SZBA宝安仓").getStockQuantity() == null
                    || stock.getWarehouseStock("SZBA宝安仓").getShippingQuantity() == null) {
                throw new IllegalStateException("Incomplete live stock for " + sku.getErpCode());
            }
            long missing = (long) demand.getOrDefault(sku.getId(), 0)
                    - stock.getWarehouseStock("SZBA宝安仓").getStockQuantity()
                    - stock.getWarehouseStock("SZBA宝安仓").getShippingQuantity();
            if (missing > 0) items.add(new SkuQuantity(sku.getId(), sku.getErpCode(), Math.toIntExact(missing)));
        }
        return items;
    }
}
