package org.jeecg.modules.business.domain.api.mabang.getshoplist;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.business.domain.api.mabang.Response;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class GetShopListResponse extends Response {
    private final Set<String> disabledShopNames;

    private GetShopListResponse(Set<String> names) {
        super(Code.SUCCESS);
        disabledShopNames = Collections.unmodifiableSet(names);
    }

    public Set<String> getDisabledShopNames() { return disabledShopNames; }

    public static GetShopListResponse parse(String json) {
        JSONObject root = JSON.parseObject(json);
        if (root == null || !"200".equals(root.getString("code"))) {
            throw new IllegalStateException("Mabang shop list request failed");
        }
        // Support both the API result envelope and a direct data array.
        Object data = root.get("data");
        JSONObject result = data instanceof JSONObject ? (JSONObject) data : root;
        JSONArray shops = result.getJSONArray("data");
        if (shops == null) {
            throw new IllegalStateException("Mabang shop list is missing data");
        }
        Integer count = result.getInteger("count");
        if (count != null && count != shops.size()) {
            throw new IllegalStateException("Mabang shop list is incomplete");
        }
        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < shops.size(); i++) {
            JSONObject shop = shops.getJSONObject(i);
            if (shop == null || StringUtils.isBlank(shop.getString("name"))
                    || !"2".equals(shop.getString("status"))) {
                throw new IllegalStateException("Invalid disabled shop in Mabang response");
            }
            names.add(shop.getString("name"));
        }
        return new GetShopListResponse(names);
    }
}