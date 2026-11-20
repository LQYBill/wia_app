package org.jeecg.modules.business.domain.job;

import org.jeecg.modules.business.entity.Sku;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jeecg.modules.business.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.quartz.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class MabangSkuStockUpdateJobTest {
    private final ISkuService skus = mock(ISkuService.class);
    private final ISkuListMabangService mabang = mock(ISkuListMabangService.class);

    private void execute(String parameter) throws Exception {
        MabangSkuStockUpdateJob job = new MabangSkuStockUpdateJob();
        ReflectionTestUtils.setField(job, "skuService", skus);
        ReflectionTestUtils.setField(job, "skuListMabangService", mabang);
        Sku a = new Sku(); a.setErpCode("A"); Sku b = new Sku(); b.setErpCode("B");
        when(skus.listSkus()).thenReturn(Arrays.asList(a, b));
        JobDataMap data = new JobDataMap(); data.put("parameter", parameter);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        job.execute(context);
    }
    private void verifyBothSteps(List<String> expected) {
        InOrder order = inOrder(mabang);
        order.verify(mabang).mabangSkuStockUpdate(expected);
        order.verify(mabang).syncAutoPurchaseLabels(expected);
        order.verifyNoMoreInteractions();
    }
    private Sku stopped(String code) {
        Sku sku = new Sku(); sku.setErpCode(code); sku.setStatus(5); return sku;
    }
    @Test void synchronizesLabelsForEntireDefaultScopeEvenWhenStockUpdateHasNoChanges() throws Exception {
        execute(null);
        verifyBothSteps(Arrays.asList("A", "B"));
    }
    @Test void explicitSkusRestrictBothStockAndLabels() throws Exception {
        execute("{\"skus\":[\"PS46000013-FC\"]}");
        verifyBothSteps(Collections.singletonList("PS46000013-FC"));
    }
    @Test void clientFilterTakesPrecedenceForBothSteps() throws Exception {
        when(skus.fetchAllClientSkuCodes("CLIENT_A")).thenReturn(Collections.singletonList("CLIENT-SKU"));
        execute("{\"clients\":[\"CLIENT_A\"],\"skus\":[\"IGNORED\"]}");
        verifyBothSteps(Collections.singletonList("CLIENT-SKU"));
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test void defaultScopeExcludesStoppedSellingBeforeBothApis() throws Exception {
        when(skus.list(any(Wrapper.class))).thenReturn(Collections.singletonList(stopped("B")));
        execute(null);
        verifyBothSteps(Collections.singletonList("A"));
        ArgumentCaptor<QueryWrapper<Sku>> query = ArgumentCaptor.forClass((Class) QueryWrapper.class);
        verify(skus).list(query.capture());
        assertEquals("erp_code", query.getValue().getSqlSelect());
        assertTrue(query.getValue().getSqlSegment().contains("status ="));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue(5));
    }
    @Test void explicitScopeFiltersStoppedCodesCaseInsensitivelyAndDeduplicates() throws Exception {
        when(skus.list(any(Wrapper.class))).thenReturn(Collections.singletonList(stopped("STOPPED")));
        execute("{\"skus\":[\"A\",\"stopped\",\"A\",\"\"]}");
        verifyBothSteps(Collections.singletonList("A"));
    }
    @Test void clientScopeAlsoExcludesStoppedCodes() throws Exception {
        when(skus.fetchAllClientSkuCodes("CLIENT_A")).thenReturn(Arrays.asList("A", "STOPPED", "A"));
        when(skus.list(any(Wrapper.class))).thenReturn(Collections.singletonList(stopped("STOPPED")));
        execute("{\"clients\":[\"CLIENT_A\"]}");
        verifyBothSteps(Collections.singletonList("A"));
    }
    @Test void allStoppedSkusCauseNoMabangRequests() throws Exception {
        when(skus.list(any(Wrapper.class))).thenReturn(Arrays.asList(stopped("A"), stopped("B")));
        execute(null);
        verifyNoInteractions(mabang);
    }
    @Test void emptyScopeSkipsDatabaseFilterAndMabangRequests() throws Exception {
        execute("{\"skus\":[]}");
        verify(skus, never()).list(any(Wrapper.class));
        verifyNoInteractions(mabang);
    }
    @Test void largeClientScopeUsesBoundedDatabaseBatches() throws Exception {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 1001; i++) codes.add("SKU-" + i);
        when(skus.fetchAllClientSkuCodes("CLIENT_A")).thenReturn(codes);
        execute("{\"clients\":[\"CLIENT_A\"]}");
        verify(skus, times(2)).list(any(Wrapper.class));
        verifyBothSteps(codes);
    }
}
