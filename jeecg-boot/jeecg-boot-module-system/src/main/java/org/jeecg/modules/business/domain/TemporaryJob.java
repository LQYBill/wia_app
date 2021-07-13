package org.jeecg.modules.business.domain;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.*;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderStatus.*;

@Slf4j
public class TemporaryJob implements Job {

    @Autowired
    @Setter
    private IPlatformOrderMabangService platformOrderMabangService;

    private static final Integer DEFAULT_NUMBER_OF_DAYS = 5;
    private static final List<OrderStatus> DEFAULT_STATUSES = Arrays.asList(AllUnshipped, Shipped, Completed);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int days = DEFAULT_NUMBER_OF_DAYS;
        List<OrderStatus> statuses = DEFAULT_STATUSES;
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String parameter = ((String) jobDataMap.get("parameter"));
        if (parameter != null) {
            try {
                JSONObject jsonObject = new JSONObject(parameter);
                JSONArray statusCodes = jsonObject.getJSONArray("status");
                if (null != statusCodes) {
                    statuses = new ArrayList<>();
                    for (int i = 0; i < statusCodes.length(); i++) {
                        statuses.add(OrderStatus.fromCode(statusCodes.getInt(i)));
                    }
                }
                if (!jsonObject.isNull("days")) {
                    days = jsonObject.getInt("days");
                }
            } catch (JSONException e) {
                log.error("Error while parsing parameter as JSON, falling back to default parameters.");
            }
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(1);
        try {
            for (int i = 0; i < days; i++) {
                for (OrderStatus status : statuses) {
                    OrderListRequestBody body = OrderListRequestBodys.allOrderOfPaidDateOfStatus(start, end, status);
                    OrderListRawStream rawStream = new OrderListRawStream(body);
                    OrderListStream stream = new OrderListStream(rawStream);
                    List<Order> unshipped = stream.all();
                    log.info("{} {} orders from {} to {} to be inserted/updated.", unshipped.size(), status, start, end);
                    platformOrderMabangService.saveOrderFromMabang(unshipped);
                }

                end = end.minusDays(1);
                start = start.minusDays(1);
            }
        } catch (OrderListRequestErrorException e) {
            throw new RuntimeException(e);
        }
    }

}
