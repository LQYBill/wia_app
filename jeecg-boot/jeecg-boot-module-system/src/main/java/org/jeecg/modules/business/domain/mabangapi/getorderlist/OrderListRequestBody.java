package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class OrderListRequestBody {
    private static final int DEV_ID = 100490;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private OrderStatus status;
    private List<String> platformOrderIds;
    private DateType datetimeType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String canSend;
    private Integer page = 1;

    JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("developerId", DEV_ID);
        json.put("timestamp", new Date().getTime() / 1000);
        json.put("action", "get-order-list");
        putNonNull(json, "status", status, OrderStatus::getCode);
        putNonNull(json, "platformOrderIds", platformOrderIds, (ids) -> String.join(",", ids));
        if(datetimeType != null){
            putNonNull(json, datetimeType.text() + "Start", startDate, formatter::format);
            putNonNull(json, datetimeType.text() + "End", endDate, formatter::format);
        }
        putNonNull(json, "canSend", canSend);
        putNonNull(json, "page", page);
        return json;
    }

    void nextPage() {
        setPage(this.page + 1);
    }

    int getPage() {
        return page;
    }


    public OrderListRequestBody setStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public OrderListRequestBody setPlatformOrderIds(List<String> platformOrderIds) {
        this.platformOrderIds = platformOrderIds;
        return this;
    }

    public OrderListRequestBody setDatetimeType(DateType datetimeType) {
        this.datetimeType = datetimeType;
        return this;
    }

    public OrderListRequestBody setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
        return this;
    }

    public OrderListRequestBody setEndDate(LocalDateTime endDate) {
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
