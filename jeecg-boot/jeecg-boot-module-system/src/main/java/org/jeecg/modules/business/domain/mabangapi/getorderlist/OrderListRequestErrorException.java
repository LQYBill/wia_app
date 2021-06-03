package org.jeecg.modules.business.domain.mabangapi.getorderlist;

/**
 * This class represents error that is returned by target get-order-list API
 * Message will contain error details.
 */
public class OrderListRequestErrorException extends Exception {
    public OrderListRequestErrorException(String msg) {
        super(msg);
    }
}
