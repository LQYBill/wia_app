package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew.*;
import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.service.ISkuService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SkuStockAutoPurchaseLabelSyncTest {
    private final ISkuService skus = mock(ISkuService.class);

    private SkuListMabangServiceImpl service(Function<SkuListRequestBody, List<SkuData>> response) {
        SkuListMabangServiceImpl service = new SkuListMabangServiceImpl() {
            @Override protected List<SkuData> fetchAutoPurchaseSkuLabels(SkuListRequestBody body) {
                return response.apply(body);
            }
        };
        ReflectionTestUtils.setField(service, "skuService", skus);
        return service;
    }
    private SkuData sku(String code, String labelName) {
        SkuData data = new SkuData(); data.setErpCode(code); data.setStatus(4);
        if (labelName != null) {
            Label label = new Label(); label.setName(labelName); data.setLabel(new Label[]{label});
        }
        return data;
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test void enablesOnlyTaggedRequestedCodesAndUpdatesOnlyTheFlag() {
        service(body -> {
            assertEquals("1", body.parameters().getString("showLabel"));
            assertEquals(50, body.getMaxRows());
            assertNull(body.getStartDate()); assertNull(body.getEndDate());
            return Arrays.asList(sku("PS46000013-FC", "缺货采购"), sku("PLAIN", null),
                    sku("OTHER", "缺货采购暂停"), sku("OUTSIDE-SCOPE", "缺货采购"));
        }).syncAutoPurchaseLabels(Arrays.asList("PS46000013-FC", "PLAIN", "OTHER"));
        ArgumentCaptor<UpdateWrapper<Sku>> captor = ArgumentCaptor.forClass((Class) UpdateWrapper.class);
        verify(skus).update(captor.capture());
        UpdateWrapper<Sku> update = captor.getValue();
        assertTrue(update.getSqlSet().startsWith("auto_purchase_enabled="));
        assertFalse(update.getSqlSet().contains(","));
        assertTrue(update.getSqlSegment().contains("erp_code IN"));
        assertTrue(update.getSqlSegment().contains("auto_purchase_enabled IS NULL"));
        Collection<Object> values = update.getParamNameValuePairs().values();
        assertTrue(values.contains("PS46000013-FC")); assertTrue(values.contains(1)); assertTrue(values.contains(0));
        assertFalse(values.contains("PLAIN")); assertFalse(values.contains("OTHER")); assertFalse(values.contains("OUTSIDE-SCOPE"));
    }
    @Test void batchesAtFiftyAndDeduplicatesSkuCodes() {
        List<Integer> sizes = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 51; i++) codes.add("SKU-" + i);
        codes.add("SKU-0"); codes.add(null); codes.add(" ");
        service(body -> {
            sizes.add(body.getStockSkuList().split(",").length);
            return Collections.emptyList();
        }).syncAutoPurchaseLabels(codes);
        assertEquals(Arrays.asList(50, 1), sizes);
        verifyNoInteractions(skus);
    }
    @Test void missingOrUnrelatedLabelsNeverClearExistingFlags() {
        service(body -> Arrays.asList(sku("A", null), sku("B", "其他")))
                .syncAutoPurchaseLabels(Arrays.asList("A", "B"));
        verifyNoInteractions(skus);
    }
    @Test void emptyScopeDoesNotCallMabangOrDatabase() {
        service(body -> { fail("No request expected"); return Collections.emptyList(); })
                .syncAutoPurchaseLabels(Collections.emptyList());
        verifyNoInteractions(skus);
    }
    @Test void apiFailurePropagatesWithoutClearingFlags() {
        assertThrows(IllegalStateException.class, () -> service(body -> {
            throw new IllegalStateException("Mabang unavailable");
        }).syncAutoPurchaseLabels(Collections.singletonList("A")));
        verifyNoInteractions(skus);
    }
}
