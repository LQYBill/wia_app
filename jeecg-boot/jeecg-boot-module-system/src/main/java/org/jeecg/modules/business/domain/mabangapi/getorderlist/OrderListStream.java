package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class provide stream of order.
 */
@Slf4j
public class OrderListStream implements DataStream<Order> {

    private final DataStream<OrderListResponse> rawStream;

    private List<Order> orders;

    private int index;

    /**
     * Flag of next data existence, use enveloped type to enable null value.
     */
    private Boolean hasNext;

    /**
     * Flag of current data is already empty,
     * either currentOrders is null or currentIndex arrives at the end.
     * In both case, we should call next() of the rawStream.
     */
    private boolean empty;


    public OrderListStream(DataStream<OrderListResponse> rawStream) {
        this.rawStream = rawStream;
        orders = null;
        this.index = 0;
        this.hasNext = null;
        this.empty = true;
    }

    @Override
    public Order begin() {
        OrderListResponse response = rawStream.begin();
        if (response == null) {
            return null;
        }
        if (response.getDataCount() == 0) {
            return null;
        }
        orders = response.getData().toJavaList(Order.class);
        index = 1;
        return orders.get(0);
    }

    @Override
    public boolean hasNext() {
        // the first time
        if (this.orders == null && rawStream.hasNext()) {
            this.empty = true;
            this.hasNext = true;
            return true;
        }

        // Current data is not yet empty
        if (index < orders.size()) {
            log.debug("Current order list is not empty yet");
            this.hasNext = true;
            return true;
        }

        /* Current data is empty */
        this.empty = true;
        log.debug("Current order list is already empty,");
        // and raw stream is empty too.
        if (!rawStream.hasNext()) {
            log.debug("and source stream is empty too, hasNext: false");
            this.hasNext = false;
            return false;
        }
        // but raw stream not empty.
        else {
            log.debug("but source stream still has data, hasNext: true");
            this.hasNext = true;
            return true;
        }
    }

    @Override
    public Order next() {
        if (hasNext == null) {
            throw new IllegalStateException("Calling next before hasNext!");
        }
        if (!hasNext) {
            throw new NoSuchElementException("Stream is empty!");
        }
        if (empty) {
            orders = this.rawStream.next().getData().toJavaList(Order.class);
            empty = false;
            index = 0;
        }
        log.debug("Return data at {}", index);
        Order res = orders.get(index);
        index++;
        hasNext = null;
        return res;
    }
}
