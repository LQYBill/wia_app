package org.jeecg.modules.business.domain.jtapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public class JTParcelTrace {

    @JsonProperty("thirdBillCode")
    private String thirdBillCode;

    @JsonProperty("orderNo")
    private String orderNo;

    @JsonProperty("billcode")
    private String billCode;

    @JsonProperty("country")
    private String country;

    @JsonProperty("details")
    private List<JTParcelTraceDetail> traceDetails;


    public JTParcelTrace(String thirdBillCode, String orderNo, String billCode, String country, List<JTParcelTraceDetail> traceDetails) {
        this.thirdBillCode = thirdBillCode;
        this.orderNo = orderNo;
        this.billCode = billCode;
        this.country = country;
        this.traceDetails = traceDetails;
    }

    public JTParcelTrace() {
    }
}
