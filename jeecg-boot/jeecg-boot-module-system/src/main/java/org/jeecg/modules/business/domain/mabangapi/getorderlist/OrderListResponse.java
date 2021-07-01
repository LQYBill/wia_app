package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * Immutable object
 */
@Slf4j
public class OrderListResponse {
    public final static String ERROR_CODE = "999";
    public final static String SUCCESS_CODE = "000";

    /**
     * total page number
     */
    private final int pageCount;
    /**
     * Total data number
     */
    private final int dataCount;
    /**
     * Current page data
     */
    private final JSONArray data;

    private final JSONObject rawData;

    OrderListResponse(int pageCount, int dataCount, JSONArray data, JSONObject rawData) {
        this.pageCount = pageCount;
        this.dataCount = dataCount;
        this.data = data;
        this.rawData = rawData;
    }

    /**
     * Make an instance by parsing json, it only checks validity of code.
     * if json is not valid, return null
     *
     * @param json the json to parse
     * @return Instance
     * @throws OrderListRequestErrorException if response code represents error.
     */
    public static OrderListResponse parse(JSONObject json) throws OrderListRequestErrorException {
        log.debug("Constructing a response by json.");
        String code = json.getString("code");
        if (code.equals(ERROR_CODE))
            throw new OrderListRequestErrorException(json.getString("message"));
        int pageCount = Integer.parseInt(json.getString("pageCount"));
        int dataCount = Integer.parseInt(json.getString("dataCount"));
        JSONArray data = json.getJSONArray("data");
        log.info("Constructed response: data contained {}, total page {}, total data {}", data.size(), pageCount, dataCount);
        return new OrderListResponse(pageCount, dataCount, data, json);
    }

    public int getTotalPage() {
        return pageCount;
    }

    public JSONArray getData() {
        return data;
    }

    public JSONObject getRawDate() {
        return rawData;
    }

    public int getDataCount() {
        return dataCount;
    }

    @Override
    public String toString() {
        return "OrderListResponse{" +
                "pageCount=" + pageCount +
                ", dataCount=" + dataCount +
                '}';
    }
}
