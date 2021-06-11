package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RetrieveOrderListJob implements Job {
    @Autowired
    private PlatformOrderMapper platformOrderMapper;
    @Autowired
    private PlatformOrderContentMapper platformOrderContentMapper;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 1. request orders that newly paid in last 30 minutes and save them to db.
        // 2. request orders that newly updated in last 30 minutes,
    }

    /**
     * Retrieve last 30 minutes newly paid order.
     */
    public void updateNewOrder() {
        OrderListRequestBody body = new OrderListRequestBody();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime begin = end.minusMinutes(30);

        body.setDatetimeType(DateType.PAID)
                .setStartDate(begin)
                .setEndDate(end);

        OrderListRequest request = new OrderListRequest(body);

    }

    /**
     * Retrieve changed order and merge them.
     */
    public void updateChangedOrder() {
        // 1. query for orders that updated last 30 minutes,
        // 2. select those that are canceled, matching its content
        // with those whose content is increased and content is the same
    }


    /**
     * Parse the json array data to image between order and its items
     *
     * @param data the data to parse
     * @return the image between platform order and its elements
     */
    public static Map<Order, List<OrderItem>> parseData(JSONArray data) {
        Map<Order, List<OrderItem>> res = new HashMap<>();

        for (JSONObject dataEntry : data.toJavaList(JSONObject.class)) {
            Order order = dataEntry.toJavaObject(Order.class);
            List<OrderItem> items = dataEntry.getJSONArray("orderItem").toJavaList(OrderItem.class);
            items.forEach(item -> item.setPlatformOrderId(order.getId()));
            res.put(order, items);
        }
        return res;
    }
}
