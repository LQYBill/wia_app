package org.jeecg.modules.business.vo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ShippingInvoiceParam {

    private final String clientID;

    private final List<String> shopIDs;

    private final String start;

    private final String end;

    private final static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public ShippingInvoiceParam(String clientID, List<String> shopIDs, String start, String end) {
        this.clientID = clientID;
        this.shopIDs = shopIDs;
        this.start = start;
        this.end = end;
    }

    public String clientID() {
        return clientID;
    }

    public List<String> shopIDs() {
        return shopIDs;
    }

    public Date start() throws ParseException {
        return format.parse(start);
    }

    public Date end() throws ParseException {
        return format.parse(end);
    }

    @Override
    public String toString() {
        return "ShippingInvoiceParam{" + clientID +
                ", shopIDs=" + shopIDs +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}