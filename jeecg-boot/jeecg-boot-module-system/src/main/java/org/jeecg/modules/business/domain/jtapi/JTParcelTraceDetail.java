package org.jeecg.modules.business.domain.jtapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;

@Slf4j
@Data
public class JTParcelTraceDetail {

    @JsonProperty("scantime")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.S")
    private String scanTime;

    @JsonProperty("scantype")
    private String scanType;

    @JsonProperty("desc")
    private String descriptionCn;

    @JsonProperty("descriptionEn")
    private String descriptionEn;

    public JTParcelTraceDetail(String scanTime, String scanType, String descriptionCn, String descriptionEn) {
        this.scanTime = scanTime;
        this.scanType = scanType;
        this.descriptionCn = descriptionCn;
        this.descriptionEn = descriptionEn;
    }

    public JTParcelTraceDetail() {
    }
}
