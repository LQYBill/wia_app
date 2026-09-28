package org.jeecg.modules.business.service.impl.purchase;

import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Short-lived cache only; missing/partial supplier responses must never mean free shipping. */
@Service
public class MabangPurchaseSupplierService {
    private final StringRedisTemplate redis;

    public MabangPurchaseSupplierService(StringRedisTemplate redis) { this.redis = redis; }

    public Map<String, String> suppliers(Collection<String> codes) throws UserException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        Map<String, String> result = new HashMap<>();
        List<String> missing = new ArrayList<>();
        for (String code : new TreeSet<>(codes)) {
            String supplier = redis.opsForValue().get("purchase:supplier:v1:" + code);
            if (supplier == null) missing.add(code); else result.put(code, supplier);
        }
        try {
            for (int offset = 0; offset < missing.size(); offset += 50) {
                if (System.nanoTime() >= deadline) throw new UserException("Supplier lookup timed out. Please retry.");
                List<String> batch = missing.subList(offset, Math.min(offset + 50, missing.size()));
                List<SkuData> fetched = fetch(batch);
                Map<String, String> batchResult = new HashMap<>();
                for (SkuData sku : fetched) {
                    if (!batch.contains(sku.getErpCode())) continue;
                    String supplier = sku.getSupplier();
                    if (supplier == null || supplier.trim().isEmpty() || "临时供货商".equals(supplier.trim())) {
                        throw new UserException("Supplier information is missing for SKU %s", sku.getErpCode());
                    }
                    supplier = supplier.trim();
                    String previous = batchResult.put(sku.getErpCode(), supplier);
                    if (previous != null && !previous.equals(supplier)) {
                        throw new UserException("Conflicting supplier information for SKU %s", sku.getErpCode());
                    }
                }
                if (batchResult.size() != batch.size()) {
                    throw new UserException("Mabang returned incomplete SKU supplier information. Please retry later.");
                }
                for (Map.Entry<String, String> entry : batchResult.entrySet()) {
                    redis.opsForValue().set("purchase:supplier:v1:" + entry.getKey(), entry.getValue(), 180, TimeUnit.SECONDS);
                }
                result.putAll(batchResult);
            }
        } catch (UserException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new UserException("Unable to obtain Mabang supplier information. Please retry later.");
        }
        return result;
    }

    protected List<SkuData> fetch(List<String> codes) {
        SkuListRequestBody body = new SkuListRequestBody().setSkuStockList(String.join(",", codes))
                .setShowProvider(1).setDatetimeType(DateType.CREATE).setTotal(50);
        body.setMaxRows(50);
        SkuListResponse response = new SkuListRequest(body).sendOnce();
        // Exact SKU batches fit one page. An unexpected partial response is rejected by the caller.
        if (response.getData() == null) return Collections.emptyList();
        List<SkuData> result = response.getData().toJavaList(SkuData.class);
        result.removeIf(sku -> sku.getStatus() != SkuStatus.Normal);
        return result;
    }
}
