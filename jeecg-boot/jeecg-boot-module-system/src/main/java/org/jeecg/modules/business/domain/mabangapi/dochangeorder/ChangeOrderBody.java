package org.jeecg.modules.business.domain.mabangapi.dochangeorder;

import org.jeecg.modules.business.domain.mabangapi.RequestBody;

import java.util.HashMap;
import java.util.Map;

public class ChangeOrderBody implements RequestBody {
    private final String platformOrderId;
    private final Map<MainParameter, String> params;

    public ChangeOrderBody(String platformOrderId) {
        this.platformOrderId = platformOrderId;
        params = new HashMap<>();
    }

    @Override
    public String action() {
        return "do-change-order";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> res = new HashMap<>();
        res.put("platformOrderId", platformOrderId);
        params.forEach(
                (key, value) -> res.put(key.name(), value)
        );

        return res;
    }

    public void setParameter(MainParameter key, String value) {
        params.put(key, value);
    }
}
