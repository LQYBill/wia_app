package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Necessary order item data from mabang side.
 */
@Data
public class OrderItem {

    @JSONField(name = "stockSku")
    private String erpCode;

    @JSONField(deserialize = false)
    private String platformOrderId;

    @JSONField(name = "quantity")
    private int quantity;
}
