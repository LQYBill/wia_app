package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;

import static org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderListRequest.sendRequest;

/**
 * This stream control reception of the response of the mabang order list API
 */
@Slf4j
public class OrderListRawStream implements DataStream<OrderListResponse> {
    /**
     * Instance's current request.
     */
    private final OrderListRequestBody toSend;
    /**
     * Response of last request.
     */
    private OrderListResponse currentResponse;

    private Boolean hasNext;

    public OrderListRawStream(OrderListRequestBody firstBody) {
        this.toSend = firstBody;
        this.currentResponse = null;
        this.hasNext = null;
    }

    /**
     * Check whether there are still order left, network communication is done here.
     *
     * @return true if there are, otherwise false.
     */
    @Override
    public boolean hasNext() throws OrderListRequestErrorException {
        // if never sent request, true
        if (currentResponse == null) {
            log.debug("current response is null, has next");
            this.hasNext = true;
            return true;
        }
        // still has page left, true
        if (toSend.getPage() <= currentResponse.getTotalPage()) {
            log.debug("page: {}/{}, has next", toSend.getPage(), currentResponse.getTotalPage());
            this.hasNext = true;
            return true;
        }
        // no page left, false
        log.debug("No page left, end");
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

        log.debug("Sending request for page {}.", toSend.getPage());
        this.currentResponse = sendRequest(toSend);
        toSend.nextPage();
        this.hasNext = null;
        return this.currentResponse;
    }
}
