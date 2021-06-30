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

    public OrderListRawStream(OrderListRequestBody firstBody) {
        this.toSend = firstBody;
        this.currentResponse = null;
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
            log.trace("Current response is null");
            return true;
        }
        // still has page left, true
        if (toSend.getPage() <= currentResponse.getTotalPage()) {
            log.trace("page: {}/{}, ", toSend.getPage(), currentResponse.getTotalPage());
            return true;
        }
        // no page left, false
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
        if (!hasNext())
            throw new NoSuchElementException();
        this.currentResponse = sendRequest(toSend);
        toSend.nextPage();
        return this.currentResponse;
    }
}
