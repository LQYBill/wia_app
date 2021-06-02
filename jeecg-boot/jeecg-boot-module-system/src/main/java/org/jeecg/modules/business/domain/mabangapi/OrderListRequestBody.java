package org.jeecg.modules.business.domain.mabangapi;

import com.alibaba.fastjson.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class OrderListRequestBody {
    private static final int DEV_ID = 100490;
    private static final DateFormat API_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private OrderStatus status;
    private List<String> platformOrderIds;
    private DateType startDateType;
    private Date startDate;
    private DateType endDateType;
    private Date endDate;
    private String canSend;
    private Integer page;

    JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("developerId", DEV_ID);
        json.put("timestamp", new Date().getTime() / 1000);
        json.put("action", "get-order-list");
        putNonNull(json, "status", status, OrderStatus::getCode);
        putNonNull(json, "platformOrderIds", platformOrderIds, (ids) -> String.join(",", ids));
        putNonNull(json, startDateType.text() + "Start", startDate, API_DATE_FORMAT::format);
        putNonNull(json, endDateType.text() + "End", endDate, API_DATE_FORMAT::format);
        putNonNull(json, "canSend", canSend);
        putNonNull(json, "page", page);
        return json;
    }

    public OrderListRequestBody setStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public OrderListRequestBody setPlatformOrderIds(List<String> platformOrderIds) {
        this.platformOrderIds = platformOrderIds;
        return this;
    }

    public OrderListRequestBody setStartDateType(DateType startDateType) {
        this.startDateType = startDateType;
        return this;
    }

    public OrderListRequestBody setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public OrderListRequestBody setEndDateType(DateType endDateType) {
        this.endDateType = endDateType;
        return this;
    }

    public OrderListRequestBody setEndDate(Date endDate) {
        this.endDate = endDate;
        return this;
    }

    public OrderListRequestBody setCanSend(String canSend) {
        this.canSend = canSend;
        return this;
    }

    public OrderListRequestBody setPage(int page) {
        this.page = page;
        return this;
    }

    private <E> void putNonNull(JSONObject json, String key, E value) {
        if (value != null) {
            json.put(key, value);
        }
    }

    private <E, T> void putNonNull(JSONObject json, String key, E value, Function<E, T> mapper) {
        if (value != null) {
            json.put(key, mapper.apply(value));
        }
    }
}
