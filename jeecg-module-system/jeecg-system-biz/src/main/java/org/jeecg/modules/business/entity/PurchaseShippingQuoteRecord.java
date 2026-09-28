package org.jeecg.modules.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/** Unconsumed quotes expire; consumed quotes are the immutable order audit snapshot. */
@Data
@TableName("purchase_shipping_quote")
public class PurchaseShippingQuoteRecord {
    @TableId
    private String id;
    private String clientId;
    private String snapshotJson;
    private Date expiresAt;
    private String purchaseOrderId;
    private Date createTime;
}
