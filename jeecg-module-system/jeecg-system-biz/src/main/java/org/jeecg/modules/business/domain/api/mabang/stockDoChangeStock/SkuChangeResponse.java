package org.jeecg.modules.business.domain.api.mabang.stockDoChangeStock;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.mabang.Response;

/**
 * Immutable object
 */
@Slf4j
@Getter
public class SkuChangeResponse extends Response {
    /**
     * Current page data
     */
    private final JSONObject data;

    private final String stockId;

    private final String stockSku;

    public SkuChangeResponse(Code code, JSONObject data, String stockId, String stockSku) {
        super(code);
        this.data = data;
        this.stockId = stockId;
        this.stockSku = stockSku;
    }

    /**
     * Make an instance by parsing json, it only checks validity of code.
     * if json is not valid, return null
     *
     * @param json the json to parse
     * @return Instance
     * @throws SkuChangeRequestErrorException if response code represents error.
     */
    public static SkuChangeResponse parse(JSONObject json, String erpCode) throws SkuChangeRequestErrorException {
        log.debug("Constructing a response by json.");
        String code = json.getString("code");
        if (!Code.SUCCESS.value.equals(code))
            throw new SkuChangeRequestErrorException(json.getString("message"));

        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            throw new SkuChangeRequestErrorException("马帮返回成功码但缺少更新结果 data");
        }
        String stockId = data.getString("stockId");
        log.info("Constructed response: data contained {}", data);
        return new SkuChangeResponse(Code.SUCCESS, data, stockId, erpCode);
    }



    @Override
    public String toString() {
        return "SkuChangeResponse{" +
                ", data=" + data +
                '}';
    }
}
