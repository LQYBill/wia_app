package org.jeecg.modules.business.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SkuQuantity {
    @JSONField(name = "id")
    private final String ID;
    @JSONField(name = "erpCode")
    private final String erpCode;
    @JSONField(name = "quantity")
    private final Integer quantity;

    @JsonCreator
    public SkuQuantity(@JsonProperty("id") String ID,
                       @JsonProperty("erpCode") String erpCode,
                       @JsonProperty("quantity") Integer quantity){
        this.ID = ID;
        this.erpCode = erpCode;
        this.quantity = quantity;
    }
    public SkuQuantity(String ID, Integer quantity){
        this.ID = ID;
        this.quantity = quantity;
        this.erpCode = null;
    }
    @Override
    public String toString(){
        return String.format("|Sku ID %s -- %d|", ID, quantity);
    }
}
