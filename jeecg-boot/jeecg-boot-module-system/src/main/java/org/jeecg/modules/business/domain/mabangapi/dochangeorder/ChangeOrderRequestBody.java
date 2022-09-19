package org.jeecg.modules.business.domain.mabangapi.dochangeorder;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.mabangapi.RequestBody;

import java.util.Map;
import java.util.function.Function;

public class ChangeOrderRequestBody implements RequestBody {

    private String platformOrderId;

    private Map<String, Integer> stockMap;

    private final static String DEFAULT_WAREHOUSE_NAME = "SZBA宝安仓";

    @Override
    public String api() {
        return "do-change-order";
    }

    @Override
    public JSONObject parameters() {
        JSONObject json = new JSONObject();
        putNonNull(json, "platformOrderId", platformOrderId);
        JSONArray stockDataArray = new JSONArray();
        for (Map.Entry<String, Integer> entry : stockMap.entrySet()) {
            JSONObject stockData = new JSONObject();
            stockData.put("warehouseName", DEFAULT_WAREHOUSE_NAME);
            stockData.put("stockSku", entry.getKey());
            stockData.put("quantity", entry.getValue());
            stockData.put("type", "1");
            stockDataArray.add(stockData);
        }
        json.put("stockData", stockDataArray.toJSONString());
        return json;
    }

    public String getPlatformOrderId() {
        return platformOrderId;
    }

    public void setPlatformOrderId(String platformOrderId) {
        this.platformOrderId = platformOrderId;
    }

    public Map<String, Integer> getStockMap() {
        return stockMap;
    }

    public void setStockMap(Map<String, Integer> stockMap) {
        this.stockMap = stockMap;
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
