package org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.NetworkDataStream;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkuUpdateLabelStreamTest {
    @SuppressWarnings("unchecked")
    private List<String> read(String rows) {
        NetworkDataStream<SkuListResponse> raw = mock(NetworkDataStream.class);
        when(raw.attempt()).thenReturn(SkuListResponse.parse(JSON.parseObject(
                "{\"code\":200,\"data\":{\"nextCursor\":\"\",\"data\":[" + rows + "]}}")));
        return new SkuUpdateListStream(raw).all().stream().map(SkuData::getErpCode).collect(Collectors.toList());
    }
    @Test void includesTaggedSkusAcrossStatusesWhileKeepingNormalStatusRules() {
        List<String> codes = read("{\"stockSku\":\"tagged-first\",\"status\":1,\"label\":[{\"name\":\"缺货采购\"}]},"
                + "{\"stockSku\":\"plain\",\"status\":1},"
                + "{\"stockSku\":\"tagged-next\",\"status\":4,\"label\":[{\"name\":\"缺货采购\"}]},"
                + "{\"stockSku\":\"normal\",\"status\":3}");
        assertEquals(java.util.Arrays.asList("tagged-first", "tagged-next", "normal"), codes);
    }
    @Test void filtersEachRowsOwnStatusAfterAStoppedSku() {
        assertEquals(java.util.Arrays.asList("stopped", "normal"), read(
                "{\"stockSku\":\"stopped\",\"status\":5},{\"stockSku\":\"plain\",\"status\":4},"
                        + "{\"stockSku\":\"normal\",\"status\":3}"));
    }
}
