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

    OrderListResponse(int pageCount, int dataCount, JSONArray data) {
        this.pageCount = pageCount;
        this.dataCount = dataCount;
        this.data = data;
    }

    /**
     * Make an instance by parsing json, it only checks validity of code.
     * if json is not valid, return null
     *
     * @param json the json to parse
     * @return Instance
     * @throws OrderListRequestErrorException if response code represents error.
     */
    public static OrderListResponse parse(JSONObject json) throws  OrderListRequestErrorException{
        String code = json.getString("code");
        if (code.equals(ERROR_CODE))
            throw new OrderListRequestErrorException(json.getString("message"));
        int pageCount = Integer.parseInt(json.getString("pageCount"));
        int dataCount = Integer.parseInt(json.getString("dataCount"));
        JSONArray data = json.getJSONArray("data");
        log.info("New response, page size {}, data size {}", pageCount, dataCount);
        return new OrderListResponse(pageCount, dataCount, data);
    }

    public int getTotalPage() {
        return pageCount;
    }

    public JSONArray getData() {
        return data;
    }

    public int getDataCount(){
        return dataCount;
    }

    @Override
    public String toString() {
        return "OrderListResponse{" +
                ", pageCount=" + pageCount +
                ", dataCount=" + dataCount +
                ", data=" + data +
                '}';
    }
}
