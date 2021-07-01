package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;

import static org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderListRequest.sendRequest;

/**
 * This stream control reception of the response of the mabang order list API
 */
@Slf4j
public class OrderListRawStream implements NetworkDataStream<OrderListResponse> {
    /**
     * Instance's current request.
     */
    private final OrderListRequestBody toSend;
    /**
     * Response of last request.
     */
    private OrderListResponse currentResponse;

    private Boolean hasNext;

    private boolean began;

    public OrderListRawStream(OrderListRequestBody firstBody) {
        this.toSend = firstBody;
        this.currentResponse = null;
        this.hasNext = null;
        began = false;
    }

    @Override
    public OrderListResponse attempt() {
        log.info("Begin the first request");
        this.currentResponse = sendRequest(toSend);
        began = true;
        return currentResponse;
    }

    /**
     * Check whether there are still order left, network communication is done here.
     *
     * @return true if there are, otherwise false.
     */
    @Override
    public boolean hasNext() throws OrderListRequestErrorException {
        if (!began) {
            throw new IllegalStateException("Calling hasNext before begin");
        }
        // still has page left, true
        if (toSend.getPage() <= currentResponse.getTotalPage()) {
            log.info("page: {}/{}, has next", toSend.getPage(), currentResponse.getTotalPage());
            this.hasNext = true;
            return true;
        }
        // no page left, false
        log.info("No page left, end");
        this.hasNext = false;
        return false;
    }

    /**
     * Get next Order.
     *
     * @return next order.
     * @throws NoSuchElementException         if data is already empty.
     * @throws OrderListRequestErrorException if request format is not valid.
     */
    @Override
    public OrderListResponse next() throws OrderListRequestErrorException {
        if (this.hasNext == null) {
            throw new IllegalStateException("Calling next before hasNext.");
        }
        if (!this.hasNext)
            throw new NoSuchElementException();

        log.info("Sending request for page {}.", toSend.getPage());
        this.currentResponse = sendRequest(toSend);
        toSend.nextPage();
        this.hasNext = null;
        return this.currentResponse;
    }
}
