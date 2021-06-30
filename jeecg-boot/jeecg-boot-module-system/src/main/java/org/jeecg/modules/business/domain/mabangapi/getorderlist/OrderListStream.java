package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * This class provide stream of order.
 */
@Slf4j
public class OrderListStream implements DataStream<Order> {

    private final OrderListRawStream rawStream;

    private List<Order> currentOrders;

    private int currentIndex;


    public OrderListStream(OrderListRawStream rawStream) {
        this.rawStream = rawStream;
        currentOrders = null;
        this.currentIndex = 0;
    }

    @Override
    public boolean hasNext() {
        if (!rawStream.hasNext()) {
            return false;
        }
        // the first time
        if (currentOrders == null) {
            currentOrders = rawStream.next().getData().toJavaList(Order.class);
            currentIndex = 0;
        }
        // current index doesn't arrive at the end, return true.
        if (currentIndex < currentOrders.size()) {
            log.trace("index: {}/{}", currentIndex, currentOrders.size());
            return true;
        }

        return false;
    }

    @Override
    public Order next() {
        currentIndex++;
        return currentOrders.get(currentIndex - 1);
    }
}
