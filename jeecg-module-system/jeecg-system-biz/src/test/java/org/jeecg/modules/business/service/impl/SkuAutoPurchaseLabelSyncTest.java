package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew.*;
import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.mapper.SkuListMabangMapper;
import org.jeecg.modules.business.service.ISkuService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SkuAutoPurchaseLabelSyncTest {
    private final ISkuService skus = mock(ISkuService.class);
    private final SkuListMabangMapper mapper = mock(SkuListMabangMapper.class);
    private final SkuListMabangServiceImpl service = new SkuListMabangServiceImpl();

    private Sku stored(Boolean enabled) {
        Sku sku = new Sku(); sku.setId("sku-id"); sku.setErpCode("PS46000013-FC");
        sku.setEnName("Product"); sku.setZhName("商品"); sku.setStatus(3); sku.setIsGift(2);
        sku.setAutoPurchaseEnabled(enabled);
        ReflectionTestUtils.setField(service, "skuService", skus);
        ReflectionTestUtils.setField(service, "skuListMabangMapper", mapper);
        when(mapper.searchExistence(anyList())).thenReturn(Collections.singletonList(sku));
        return sku;
    }
    private SkuData incoming() {
        SkuData data = new SkuData(); data.setErpCode("PS46000013-FC");
        data.setNameEN("Product"); data.setNameCN("商品"); data.setStatus(3); data.setIsGift(2);
        return data;
    }
    private Label label(String name) { Label label = new Label(); label.setName(name); return label; }

    @Test void labelOnlyChangeEnablesAndPersistsExistingSku() {
        Sku sku = stored(false); SkuData data = incoming();
        data.setLabel(new Label[]{label("其他"), label("缺货采购")});
        service.updateSkusFromMabang(Collections.singletonList(data));
        assertTrue(sku.getAutoPurchaseEnabled());
        verify(skus).updateBatchById(Collections.singletonList(sku));
    }
    @Test void alsoSupportsLabelDataAndNullEntries() {
        Sku sku = stored(null); SkuData data = incoming();
        data.setLabel(new Label[]{null}); data.setLabelData(new Label[]{null, label("缺货采购")});
        service.updateSkusFromMabang(Collections.singletonList(data));
        assertTrue(sku.getAutoPurchaseEnabled());
        verify(skus).updateBatchById(Collections.singletonList(sku));
    }
    @Test void absentLabelsDoNotDisablePreviouslyEnabledSku() {
        Sku sku = stored(true); SkuData data = incoming();
        service.updateSkusFromMabang(Collections.singletonList(data));
        assertTrue(sku.getAutoPurchaseEnabled());
        verify(skus, never()).updateBatchById(anyCollection());
    }
    @Test void unrelatedOrPartialLabelNamesDoNotEnableSku() {
        Sku sku = stored(false); SkuData data = incoming();
        data.setLabel(new Label[]{label("不缺货采购"), label("缺货采购暂停")});
        service.updateSkusFromMabang(Collections.singletonList(data));
        assertFalse(sku.getAutoPurchaseEnabled());
        verify(skus, never()).updateBatchById(anyCollection());
    }
    @Test void alreadyEnabledSkuDoesNotTriggerRedundantUpdate() {
        Sku sku = stored(true); SkuData data = incoming(); data.setLabel(new Label[]{label("缺货采购")});
        service.updateSkusFromMabang(Collections.singletonList(data));
        assertTrue(sku.getAutoPurchaseEnabled());
        verify(skus, never()).updateBatchById(anyCollection());
    }
}
