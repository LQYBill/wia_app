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
import org.jeecg.modules.business.mapper.ShoumanScheduledOrderErpCodeMapper;
import org.jeecg.modules.business.service.IShoumanOrderService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class ShoumanOrderErpCodeScheduledJob implements Job {
    @Autowired private ShoumanScheduledOrderErpCodeMapper orderMapper;
    @Autowired private PlatformOrderContentMapper contentMapper;
    @Autowired private IShoumanOrderService shoumanOrderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JSONObject p = params(context);
        Date begin = date(p.getString("beginOrderTime"));
        Date end = date(p.getString("endOrderTime"));
        if (begin != null && end != null && begin.after(end)) throw new JobExecutionException("开始订单时间不能晚于结束订单时间");
        List<String> numbers = numbers(p.getString("orderNumbers"));
        List<ShoumanOrder> orders = new ArrayList<>();
        for (String id : orderMapper.findOrderIds(begin, end, p.getString("shopErpCode"), numbers)) {
            ShoumanOrderBase base = contentMapper.searchShoumanOrderContentByPlatformOrderId(id);
            if (base == null) continue;
            ShoumanOrder order = new ShoumanOrder();
            order.setPlatformOrderId(id);
            order.setOrderJson(new OrderCreationRequestBody(base).parameters().toJSONString());
            order.setCreateBy("shouman scheduled job");
            orders.add(order);
        }
        if (orders.isEmpty()) return;
        shoumanOrderService.saveBatch(orders);
        for (ShoumanOrder order : orders) {
            String result = new OrderCreationRequest(new JsonOrderCreationRequestBody(order.getOrderJson())).rawSend(order).getBody();
            if (Integer.valueOf(1).equals(JSON.parseObject(result).getInteger("status"))) order.setSuccess("1");
        }
        shoumanOrderService.updateBatchById(orders);
    }

    private JSONObject params(JobExecutionContext c) { String s = c.getJobDetail().getJobDataMap().getString("parameter"); return s == null || s.trim().isEmpty() ? new JSONObject() : JSON.parseObject(s); }
    private Date date(String value) throws JobExecutionException {
        if (value == null || value.trim().isEmpty()) return null;
        for (String pattern : Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd")) try { return new SimpleDateFormat(pattern).parse(value.trim()); } catch (ParseException ignored) { }
        throw new JobExecutionException("无法解析订单时间: " + value);
    }
    private List<String> numbers(String value) { if (value == null || value.trim().isEmpty()) return Collections.emptyList(); List<String> r = new ArrayList<>(); for (String n : value.split(",")) if (!n.trim().isEmpty() && !r.contains(n.trim())) r.add(n.trim()); return r; }
}
