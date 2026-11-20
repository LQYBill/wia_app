package org.jeecg.modules.business.domain.job;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.service.ISkuListMabangService;
import org.jeecg.modules.business.service.ISkuService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
@Slf4j
@Component
public class MabangSkuStockUpdateJob implements Job {
    private static final int STOPPED_SELLING_STATUS = 5;
    @Autowired
    private ISkuListMabangService skuListMabangService;
    @Autowired
    private ISkuService skuService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Sku stock update Job has started.");
        List<String> erpCodes = skuService.listSkus().stream().map(Sku::getErpCode).collect(Collectors.toList());
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String parameter = ((String) jobDataMap.get("parameter"));
        if (parameter != null) {
            try {
                JSONObject jsonObject = new JSONObject(parameter);
                if(!jsonObject.isNull("clients")) {
                    log.info("Request to sync stock for all SKUs of clients: {}", jsonObject.getJSONArray("clients"));
                    erpCodes.clear();
                    JSONArray array = jsonObject.getJSONArray("clients");
                    for(int i = 0; i < array.length(); i++) {
                        String clientCode = array.getString(i);
                        erpCodes.addAll(skuService.fetchAllClientSkuCodes(clientCode));
                    }
                }
                if (!jsonObject.isNull("skus") && jsonObject.isNull("clients")) {
                    log.info("Request to sync stock for specific SKUs: {}", jsonObject.getJSONArray("skus"));
                    erpCodes.clear();
                    JSONArray array = jsonObject.getJSONArray("skus");
                    for(int i = 0; i < array.length(); i++) {
                        erpCodes.add(array.getString(i));
                    }
                }
            } catch (JSONException e) {
                log.error("Error while parsing parameter as JSON, falling back to default parameters.");
            }
        } else {
            log.info("No parameter provided, syncing stock for all SKUs.");
        }
        erpCodes = excludeStoppedSkus(erpCodes);
        if (erpCodes.isEmpty()) {
            log.info("No SKU codes remain after filtering stopped-selling SKUs, skipping Mabang requests.");
            return;
        }
        skuListMabangService.mabangSkuStockUpdate(erpCodes);
        skuListMabangService.syncAutoPurchaseLabels(erpCodes);
        log.info("Sku stock update Job has ended.");
    }

    private List<String> excludeStoppedSkus(List<String> requestedCodes) {
        List<String> codes = requestedCodes.stream()
                .filter(code -> code != null && !code.trim().isEmpty()).distinct().collect(Collectors.toList());
        Set<String> stoppedCodes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        // Filter locally before either API call; bounded SQL batches avoid per-SKU lookups.
        for (List<String> batch : Lists.partition(codes, 1000)) {
            for (Sku sku : skuService.list(new QueryWrapper<Sku>().select("erp_code")
                    .eq("status", STOPPED_SELLING_STATUS).in("erp_code", batch))) {
                if (sku.getErpCode() != null) stoppedCodes.add(sku.getErpCode());
            }
        }
        List<String> remaining = codes.stream().filter(code -> !stoppedCodes.contains(code)).collect(Collectors.toList());
        log.info("SKU stock sync scope: {} unique codes, {} stopped-selling excluded, {} remaining",
                codes.size(), codes.size() - remaining.size(), remaining.size());
        return remaining;
    }
}
