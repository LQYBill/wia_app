package org.jeecg.modules.business.service.impl.purchase;

import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew.SkuData;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MabangPurchaseSupplierServiceTest {
    @Test void queriesOnlyUncachedSku() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("purchase:supplier:v1:a")).thenReturn("A");
        MabangPurchaseSupplierService service = spy(new MabangPurchaseSupplierService(redis));
        SkuData sku = new SkuData(); sku.setErpCode("b"); sku.setSupplier(" B ");
        doReturn(List.of(sku)).when(service).fetch(List.of("b"));
        assertEquals(Map.of("a", "A", "b", "B"), service.suppliers(List.of("a", "b")));
        verify(service).fetch(List.of("b"));
        verify(values).set("purchase:supplier:v1:b", "B", 180, TimeUnit.SECONDS);
    }
    @Test void missingAndPartialResultsNeverBecomeFreeShipping() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        MabangPurchaseSupplierService service = spy(new MabangPurchaseSupplierService(redis));
        doReturn(Collections.emptyList()).when(service).fetch(anyList());
        assertThrows(UserException.class, () -> service.suppliers(List.of("a")));
        SkuData sku = new SkuData(); sku.setErpCode("a"); sku.setSupplier(" ");
        doReturn(List.of(sku)).when(service).fetch(anyList());
        assertThrows(UserException.class, () -> service.suppliers(List.of("a")));
        verify(values, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }
}
