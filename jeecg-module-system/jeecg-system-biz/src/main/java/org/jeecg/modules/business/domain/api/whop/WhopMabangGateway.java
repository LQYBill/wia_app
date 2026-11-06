package org.jeecg.modules.business.domain.api.whop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.api.mabang.Request;
import org.jeecg.modules.business.domain.api.mabang.RequestBody;
import org.jeecg.modules.business.domain.api.mabang.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Reuses the application's existing Mabang account and signing implementation. */
@Component
public class WhopMabangGateway {
    public JSONObject create(JSONObject order) {
        return call("order-do-create-order", order);
    }

    public JSONObject check(String paymentId) {
        JSONObject params = new JSONObject();
        params.put("platformOrderId", paymentId);
        return call("order-do-create-order-check", params);
    }

    private JSONObject call(String api, JSONObject params) {
        return new JsonRequest(new RequestBody() {
            @Override public String api() { return api; }
            @Override public JSONObject parameters() { return params; }
        }).send().json;
    }

    private static class JsonRequest extends Request {
        JsonRequest(RequestBody body) { super(body); }
        @Override public JsonResponse send() {
            // Creation is asynchronous: never retry a timeout blindly.
            ResponseEntity<String> response = rawSend(1);
            if (response == null || response.getBody() == null) {
                throw new IllegalStateException("Mabang response unavailable; reconcile before retry");
            }
            JSONObject json = JSON.parseObject(response.getBody());
            if (json == null || json.getString("code") == null) {
                throw new IllegalStateException("Invalid Mabang response");
            }
            return new JsonResponse(json);
        }
    }

    private static class JsonResponse extends Response {
        private final JSONObject json;
        JsonResponse(JSONObject json) {
            super("200".equals(json.getString("code")) ? Code.SUCCESS : Code.ERROR);
            this.json = json;
        }
    }
}
