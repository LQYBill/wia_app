package org.jeecg.modules.business.domain.mabangapi;

import com.alibaba.fastjson.JSONObject;

public interface RequestBody {
    String action();

    JSONObject parameters();

}
