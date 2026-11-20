package org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.NetworkDataStream;

import java.util.NoSuchElementException;
import java.util.HashSet;
import java.util.Set;

/**
 * This stream control reception of the response of the mabang order list API
 */
@Slf4j
public class SkuListRawStream implements NetworkDataStream<SkuListResponse> {
    /**
     * Instance's current request.
     */
    private final SkuListRequestBody toSend;
    /**
     * Response of last request.
     */
    private SkuListResponse currentResponse;

    private boolean began;
    private final Set<String> requestedCursors = new HashSet<>();

    public SkuListRawStream(SkuListRequestBody firstBody) {
        this.toSend = firstBody;
        this.currentResponse = null;
        began = false;
    }

    @Override
    public SkuListResponse attempt() {
        log.info("Begin the first request");
        requestedCursors.clear();
        requestedCursors.add(toSend.getCursor());
        this.currentResponse = sendRequest(toSend);
        if (currentResponse == null) throw new IllegalStateException("Mabang SKU lookup unavailable");
        began = true;
        if (currentResponse.getData() == null || currentResponse.getData().isEmpty()) {
            return null;
        }
        toSend.nextPage();
        return currentResponse;
    }

    /**
     * Check whether there are still order left, network communication is done here.
     *
     * @return true if there are, otherwise false.
     */
    @Override
    public boolean hasNext() throws SkuListRequestErrorException {
        if (!began) {
            throw new IllegalStateException("Calling hasNext before begin");
        }
        // no page left, false
        if (currentResponse == null || currentResponse.getData() == null || currentResponse.getData().isEmpty()
                || currentResponse.getCursor() == null || currentResponse.getCursor().trim().isEmpty()) {
            log.info("No page left, end");
            return false;
        }
        // still has page left, true
        log.info("page: {}, has next", toSend.getPage());
        if (requestedCursors.contains(currentResponse.getCursor())) {
            throw new SkuListRequestErrorException("Mabang SKU pagination returned a repeated cursor");
        }
        return true;
    }

    /**
     * Get next Order.
     *
     * @return next order.
     * @throws NoSuchElementException         if data is already empty.
     * @throws SkuListRequestErrorException if request format is not valid.
     */
    @Override
    public SkuListResponse next() throws SkuListRequestErrorException {
        if (!hasNext())
            throw new NoSuchElementException();

        log.info("Sending request for page {}/{}.", toSend.getPage(), toSend.getTotal() == null ? "?" : toSend.getTotalPages());
        toSend.setCursor(currentResponse.getCursor());
        requestedCursors.add(toSend.getCursor());
        this.currentResponse = sendRequest(toSend);
        if (currentResponse == null) throw new IllegalStateException("Mabang SKU lookup unavailable");
        toSend.nextPage();
        return this.currentResponse;
    }

    protected SkuListResponse sendRequest(SkuListRequestBody body) {
        return new SkuListRequest(body).send();
    }
}
