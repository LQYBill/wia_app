package org.jeecg.modules.business.vo;

import java.time.Instant;
import java.util.List;

public class ShippingInvoiceParam {
    private final List<String>  shopIDs;

    private final String start;

    private final String end;

    public ShippingInvoiceParam(List<String> shopIDs, String start, String end) {
        this.shopIDs = shopIDs;
        this.start = start;
        this.end = end;
    }


    public List<String> shopIDs() {
        return shopIDs;
    }

    public String start() {
        return start;
    }

    public String end() {
        return end;
    }

    @Override
    public String toString() {
        return "ShippingInvoiceParam{" +
                ", shopIDs=" + shopIDs +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
