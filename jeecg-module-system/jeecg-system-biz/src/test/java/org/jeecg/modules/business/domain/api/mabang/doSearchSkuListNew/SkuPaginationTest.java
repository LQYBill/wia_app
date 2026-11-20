package org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.business.domain.api.mabang.Response;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.NetworkDataStream;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkuPaginationTest {
    private SkuListResponse page(String code, String cursor) {
        return SkuListResponse.parse(JSON.parseObject("{\"code\":200,\"data\":{\"nextCursor\":\"" + cursor
                + "\",\"data\":[{\"stockSku\":\"" + code + "\",\"status\":3,\"label\":[{\"name\":\"缺货采购\"}]}]}}"));
    }
    private SkuListResponse nullPage() {
        // Reproduce the exact null getData() that triggered the production exception.
        return new SkuListResponse(Response.Code.SUCCESS, "unused", null, null);
    }
    @SuppressWarnings("unchecked")
    private NetworkDataStream<SkuListResponse> source() { return mock(NetworkDataStream.class); }

    @Test void updateStreamPreservesAllRowsBeforeNullFinalPage() {
        NetworkDataStream<SkuListResponse> raw = source();
        when(raw.attempt()).thenReturn(page("A", "2"));
        when(raw.hasNext()).thenReturn(true);
        when(raw.next()).thenReturn(page("B", "3"), nullPage());
        SkuUpdateListStream stream = new SkuUpdateListStream(raw);
        assertEquals(Arrays.asList("A", "B"), stream.all().stream().map(SkuData::getErpCode).collect(Collectors.toList()));
        assertFalse(stream.hasNext());
        assertThrows(NoSuchElementException.class, stream::next);
        verify(raw, times(2)).next();
    }
    @Test void ordinarySkuStreamAlsoHandlesNullFinalPage() {
        NetworkDataStream<SkuListResponse> raw = source();
        when(raw.attempt()).thenReturn(page("A", "2"));
        when(raw.hasNext()).thenReturn(true);
        when(raw.next()).thenReturn(nullPage());
        assertEquals(1, new SkuListStream(raw).all().size());
    }
    @Test void repeatedHasNextCallsDoNotFetchTwiceOrSkipRows() {
        NetworkDataStream<SkuListResponse> raw = source();
        when(raw.attempt()).thenReturn(page("A", "2"));
        when(raw.hasNext()).thenReturn(true);
        when(raw.next()).thenReturn(page("B", "3"), nullPage());
        SkuUpdateListStream stream = new SkuUpdateListStream(raw);
        assertEquals("A", stream.attempt().getErpCode());
        assertTrue(stream.hasNext()); assertTrue(stream.hasNext());
        verify(raw, times(1)).next();
        assertEquals("B", stream.next().getErpCode());
        assertFalse(stream.hasNext()); assertFalse(stream.hasNext());
        verify(raw, times(2)).next();
    }
    @Test void nullOrEmptyFirstPageLeavesIteratorExhausted() {
        for (SkuListResponse response : Arrays.asList(null, nullPage(),
                SkuListResponse.parse(JSON.parseObject("{\"code\":200,\"data\":{\"data\":[]}}")))) {
            NetworkDataStream<SkuListResponse> raw = source();
            when(raw.attempt()).thenReturn(response);
            SkuUpdateListStream stream = new SkuUpdateListStream(raw);
            assertTrue(stream.all().isEmpty()); assertFalse(stream.hasNext());
            assertThrows(NoSuchElementException.class, stream::next);
            verify(raw, never()).hasNext();
        }
    }
    @Test void missingNetworkResponseDoesNotSilentlyTruncateResults() {
        NetworkDataStream<SkuListResponse> raw = source();
        when(raw.attempt()).thenReturn(page("A", "2")); when(raw.hasNext()).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> new SkuUpdateListStream(raw).all());
    }
    @Test void normalizesSuccessfulNullDataAndMissingCursor() {
        for (String json : Arrays.asList("{\"code\":200}", "{\"code\":200,\"data\":null}",
                "{\"code\":200,\"data\":{\"data\":null}}")) {
            SkuListResponse response = SkuListResponse.parse(JSON.parseObject(json));
            assertNotNull(response.getData()); assertTrue(response.getData().isEmpty());
            assertEquals("", response.getCursor());
        }
    }
    @Test void rejectsAllNonSuccessCodesInsteadOfTreatingErrorsAsEmptyPages() {
        for (String code : Arrays.asList("0", "429", "500")) {
            SkuListRequestErrorException error = assertThrows(SkuListRequestErrorException.class,
                    () -> SkuListResponse.parse(JSON.parseObject("{\"code\":" + code + ",\"message\":\"request failed\",\"data\":null}")));
            assertTrue(error.getMessage().contains(code));
        }
        assertThrows(SkuListRequestErrorException.class, () -> SkuListResponse.parse(null));
        assertThrows(SkuListRequestErrorException.class, () -> SkuListResponse.parse(JSON.parseObject("{}")));
    }
    @Test void rawPaginationStopsAfterEmptyPageAndDoesNotAdvanceOnHasNext() {
        List<String> cursors = new ArrayList<>();
        Deque<SkuListResponse> pages = new ArrayDeque<>(Arrays.asList(page("A", "2"), page("B", "3"), nullPage()));
        SkuListRequestBody body = new SkuListRequestBody();
        SkuListRawStream raw = new SkuListRawStream(body) {
            @Override protected SkuListResponse sendRequest(SkuListRequestBody request) {
                cursors.add(request.getCursor()); return pages.removeFirst();
            }
        };
        assertNotNull(raw.attempt()); assertTrue(raw.hasNext()); assertTrue(raw.hasNext());
        assertEquals("1", body.getCursor());
        assertNotNull(raw.next()); assertTrue(raw.hasNext()); assertNotNull(raw.next());
        assertFalse(raw.hasNext());
        assertEquals(Arrays.asList("1", "2", "3"), cursors);
    }
    @Test void rawPaginationHandlesMissingCursorAndDetectsCursorLoops() {
        SkuListRawStream noCursor = new SkuListRawStream(new SkuListRequestBody()) {
            @Override protected SkuListResponse sendRequest(SkuListRequestBody body) {
                return new SkuListResponse(Response.Code.SUCCESS, null, page("A", "").getData(), null);
            }
        };
        assertNotNull(noCursor.attempt()); assertFalse(noCursor.hasNext());
        SkuListRawStream repeated = new SkuListRawStream(new SkuListRequestBody()) {
            @Override protected SkuListResponse sendRequest(SkuListRequestBody body) { return page("A", "1"); }
        };
        assertNotNull(repeated.attempt());
        assertThrows(SkuListRequestErrorException.class, repeated::hasNext);
    }
}
