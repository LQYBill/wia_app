package org.jeecg.modules.business.vo;

import lombok.Data;
import java.time.LocalDateTime;

/** A bounded SKU-code repair candidate, including its original value for optimistic updates. */
@Data
public class SkuIdBackfillRow {
    private String id;
    private String skuId;
    private LocalDateTime createTime;
}
