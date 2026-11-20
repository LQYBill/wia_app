package org.jeecg.modules.business.domain.job;

import org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew.*;
import org.jeecg.modules.business.service.ISkuListMabangService;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MabangSkuSyncJobTest {
    private void verifyRequest(String parameter, boolean bySku) throws Exception {
        ISkuListMabangService service = mock(ISkuListMabangService.class);
        SkuData sku = new SkuData(); sku.setErpCode("PS46000013-FC");
        Label label = new Label(); label.setName("缺货采购"); sku.setLabel(new Label[]{label});
        List<SkuListRequestBody> requests = new ArrayList<>();
        MabangSkuSyncJob job = new MabangSkuSyncJob() {
            @Override protected List<SkuData> fetchSkus(SkuListRequestBody body) {
                requests.add(body);
                return Collections.singletonList(sku);
            }
        };
        ReflectionTestUtils.setField(job, "skuListMabangService", service);
        when(service.updateSkusFromMabang(anyList())).thenReturn(Collections.emptyMap());
        JobDataMap data = new JobDataMap(); data.put("parameter", parameter);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        job.execute(context);
        assertEquals(1, requests.size());
        assertEquals("1", requests.get(0).parameters().getString("showLabel"));
        if (bySku) assertEquals("PS46000013-FC", requests.get(0).getStockSkuList());
        else assertNotNull(requests.get(0).getStartDate());
        verify(service).updateSkusFromMabang(Collections.singletonList(sku));
    }

    @Test void requestsLabelsWhenSyncingSpecificSkus() throws Exception {
        verifyRequest("{\"skus\":[\"PS46000013-FC\"]}", true);
    }
    @Test void requestsLabelsWhenSyncingDateRange() throws Exception {
        verifyRequest("{\"startDateTime\":\"2026-11-17T00:00:00\",\"endDateTime\":\"2026-11-18T00:00:00\"}", false);
    }
}
