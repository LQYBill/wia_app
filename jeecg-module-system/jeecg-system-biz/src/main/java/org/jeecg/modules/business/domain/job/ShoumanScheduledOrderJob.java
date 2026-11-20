package org.jeecg.modules.business.domain.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.shouman.JsonOrderCreationRequestBody;
import org.jeecg.modules.business.domain.api.shouman.OrderCreationRequest;
import org.jeecg.modules.business.domain.api.shouman.OrderCreationRequestBody;
import org.jeecg.modules.business.entity.Shouman.ShoumanOrder;
import org.jeecg.modules.business.entity.Shouman.ShoumanOrderBase;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.ShoumanScheduledOrderMapper;
import org.jeecg.modules.business.service.IShoumanOrderService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
public class ShoumanScheduledOrderJob implements Job {
    private static final List<String> DATE_PATTERNS = Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd");
    @Autowired private ShoumanScheduledOrderMapper scheduledOrderMapper;
    @Autowired private PlatformOrderContentMapper platformOrderContentMapper;
    @Autowired private IShoumanOrderService shoumanOrderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JSONObject params = parameters(context);
        Date begin = date(params, "beginOrderTime", "startOrderTime");
        Date end = date(params, "endOrderTime", "finishOrderTime");
        if (begin != null && end != null && begin.after(end)) throw new JobExecutionException("开始订单时间不能晚于结束订单时间");
        String shopId = nonBlank(params.getString("shopId"), params.getString("shop"));
        List<String> numbers = orderNumbers(params.getString("orderNumbers"), params.getString("orderNos"));
        List<ShoumanOrder> orders = new ArrayList<>();
        for (String id : scheduledOrderMapper.findOrderIds(begin, end, shopId, numbers)) {
            ShoumanOrderBase base = platformOrderContentMapper.searchShoumanOrderContentByPlatformOrderId(id);
            if (base == null) continue;
            ShoumanOrder order = new ShoumanOrder();
            order.setPlatformOrderId(id);
            order.setOrderJson(new OrderCreationRequestBody(base).parameters().toJSONString());
            order.setCreateBy("shouman scheduled job");
            orders.add(order);
        }
        if (orders.isEmpty()) { log.info("No eligible Shouman orders found"); return; }
        shoumanOrderService.saveBatch(orders);
        for (ShoumanOrder order : orders) {
            String result = new OrderCreationRequest(new JsonOrderCreationRequestBody(order.getOrderJson())).rawSend(order).getBody();
            if (Integer.valueOf(1).equals(JSON.parseObject(result).getInteger("status"))) order.setSuccess("1");
        }
        shoumanOrderService.updateBatchById(orders);
        log.info("Created and sent {} Shouman orders", orders.size());
    }

    static List<String> orderNumbers(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) {
            List<String> result = new ArrayList<>();
            for (String item : value.split(",")) if (!item.trim().isEmpty() && !result.contains(item.trim())) result.add(item.trim());
            return result;
        }
        return Collections.emptyList();
    }

    private JSONObject parameters(JobExecutionContext context) {
        String value = context.getJobDetail().getJobDataMap().getString("parameter");
        return value == null || value.trim().isEmpty() ? new JSONObject() : JSON.parseObject(value);
    }
    private Date date(JSONObject params, String... keys) throws JobExecutionException {
        String value = null;
        for (String key : keys) if (params.getString(key) != null && !params.getString(key).trim().isEmpty()) { value = params.getString(key).trim(); break; }
        if (value == null) return null;
        for (String pattern : DATE_PATTERNS) try { return new SimpleDateFormat(pattern).parse(value); } catch (ParseException ignored) { }
        throw new JobExecutionException("无法解析订单时间: " + value);
    }
    private String nonBlank(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return null;
    }
}
