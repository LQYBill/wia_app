package org.jeecg.modules.business.domain.mabangapi.getorderlist;

public enum OrderStatus {
    Pending(1),
    Purchasing(2),
    Shipped(3),
    Completed(4),
    Obsolete(5),
    AllUnshipped(6),
    AllNonUnshipped(7);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public String getCode() {
        return String.valueOf(code);
    }
}
