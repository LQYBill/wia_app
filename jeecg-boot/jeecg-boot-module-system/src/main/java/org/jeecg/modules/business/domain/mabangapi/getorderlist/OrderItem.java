package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Necessary order item data from mabang side.
 */
@Data
public class OrderItem {

    @JSONField(deserialize = false)
    private String mainKey;

    @JSONField(name = "stockSku")
    private String erpCode;

    @JSONField(name = "quantity")
    private String platformOrderId;

    @JSONField(name = "quantity")
    private int quantity;
}