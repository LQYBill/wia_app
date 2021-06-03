package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class PlatformOrderScanner {
    private final static SimpleDateFormat format = new
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Order parseDataEntry(JSONObject dataEntry) throws ParseException {
        return dataEntry.toJavaObject(Order.class);
    }

    OrderItem parseOrderItem(JSONObject orderItem, String platformOrderId) {
        OrderItem item = orderItem.toJavaObject(OrderItem.class);
        item.setMainKey(platformOrderId);
        return item;
    }




}
