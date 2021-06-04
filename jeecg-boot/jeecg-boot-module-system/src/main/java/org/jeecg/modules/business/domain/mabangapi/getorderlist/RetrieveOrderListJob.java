package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RetrieveOrderListJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    }

    public void updateDateFromMabang(){
        OrderListRequestBody body = new OrderListRequestBody();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime begin = end.minusDays(1);

        body.setDatetimeType(DateType.PAID)
                .setStartDate(begin)
                .setEndDate(end);

        OrderListRequest request = new OrderListRequest(body);
        try{
            while (request.hasNext()){
                JSONArray data = request.next();
                System.out.println(data);
                System.out.println(parseData(data));
            }
        } catch (OrderListRequestErrorException e){
            throw new RuntimeException(e.getMessage());
        }

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
