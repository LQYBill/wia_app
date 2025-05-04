package org.jeecg.modules.business.domain.api.yd;

import com.alibaba.fastjson.JSONObject;

public class YDRemoveOrderRequestBody extends YDRequestBody {

    private static final String SERVICE_METHOD = "removeorder";
    private static final String PARAM_KEY = "reference_no";

    public YDRemoveOrderRequestBody(String platformOrderNumber) {
        super(SERVICE_METHOD, generateJsonString(platformOrderNumber));
    }

    private static String generateJsonString(String platformOrderNumber) {
        JSONObject param = new JSONObject();
        param.put(PARAM_KEY, platformOrderNumber);
        return param.toJSONString();
    }
    public String toString() {
        return "YDRemoveOrderRequestBody(serviceMethod=" + this.getServiceMethod() + ", paramJson=" + this.getParamsJson() + ")";
    }
}
