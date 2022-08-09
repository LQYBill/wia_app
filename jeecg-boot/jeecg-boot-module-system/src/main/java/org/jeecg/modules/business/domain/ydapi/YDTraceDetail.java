package org.jeecg.modules.business.domain.ydapi;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;

@Slf4j
@Data
public class YDTraceDetail {

    @JSONField(deserialize = false)
    private String parcelId;

    @JsonProperty("track_occur_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.S")
    private String scanTime;

    @JsonProperty("track_description")
    private String descriptionCn;

    @JsonProperty("track_description_en")
    private String descriptionEn;

    @JsonProperty("track_location")
    private String traceLocation;

    @JsonProperty("track_status")
    private String traceStatus;

    @JsonProperty("track_code")
    private String traceCode;

    @JsonProperty("tbs_id")
    private String orderNo;

    @JsonProperty("track_server_code")
    private String productCode;

    public YDTraceDetail() {
    }

    public boolean equals(Object anotherDetail) {
        if (anotherDetail instanceof YDTraceDetail) {
            YDTraceDetail another = (YDTraceDetail) anotherDetail;
            return another.getParcelId().equalsIgnoreCase(this.parcelId)
                    && another.getTraceCode().equalsIgnoreCase(this.traceCode)
                    && another.getScanTime().equalsIgnoreCase(this.scanTime)
                    && another.getTraceLocation().equalsIgnoreCase(this.traceLocation);
        }
        return false;
    }
}
