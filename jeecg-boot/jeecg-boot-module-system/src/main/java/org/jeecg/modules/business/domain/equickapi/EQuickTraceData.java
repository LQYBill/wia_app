package org.jeecg.modules.business.domain.equickapi;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.jtapi.JTParcelTrace;

import java.util.List;

@Slf4j
@Data
public class EQuickTraceData {

    @JSONField(deserialize = false)
    private String parcelId;

    @JSONField(deserialize = false)
    private String scanType;

    /**
     * +
     * 产品代码
     */
    @JsonProperty("QuickType")
    private String quickType;

    /**
     * 国外标签单号
     */
    @JsonProperty("TraceLabelNo")
    private String traceLabelNo;

    /**
     * >=200 – 国外部分 >=100<=199 – 国内部分 特别关注： 299 – 妥投 298 – 派送异常
     */
    @JsonProperty("TraceKind")
    private Integer traceKind;

    /**
     * 追踪时间
     */
    @JsonProperty("TraceDateTime")
    private String traceDateTime;

    /**
     * 追踪地点
     */
    @JsonProperty("TraceLocation")
    private String traceLocation;

    /**
     * 追踪说明
     */
    @JsonProperty("TraceContent")
    private String traceContent;

    /**
     * 追踪国家
     */
    @JsonProperty("TraceCountry")
    private String traceCountry;

    /**
     * 追踪状态
     */
    @JsonProperty("TraceStatus")
    private String traceStatus;

    /**
     * 目的地单号
     */
    @JsonProperty("DestinationWBNo")
    private String destinationWBNo;

    public EQuickTraceData(String quickType, String traceLabelNo, Integer traceKind, String traceDateTime, String traceLocation, String traceContent, String traceCountry, String traceStatus, String destinationWBNo) {
        this.quickType = quickType;
        this.traceLabelNo = traceLabelNo;
        this.traceKind = traceKind;
        this.traceDateTime = traceDateTime;
        this.traceLocation = traceLocation;
        this.traceContent = traceContent;
        this.traceCountry = traceCountry;
        this.traceStatus = traceStatus;
        this.destinationWBNo = destinationWBNo;
    }

    public EQuickTraceData() {
    }

    /**
     * Set parcel ID, and add scan type info accordingly
     *
     * @param parcelId Parcel ID to which the trace belongs to
     */
    public void parcelTraceProcess(String parcelId) {
        setParcelId(parcelId);
        switch (traceKind) {
            case 111:
                setScanType("Order Placed");
                break;
            case 112:
                setScanType("Waiting for Delivery");
                break;
            case 123:
                setScanType("Received by Consolidation Warehouse");
                break;
            case 131:
                setScanType("Consolidation Center Outbound");
                break;
            case 141:
                setScanType("Consolidation Center Dispatch");
                setTraceContent("Arrived at carrier operation center");
                break;
            case 151:
                setScanType("Aviation Stowage");
                setTraceContent("Departed from carrier operation center");
                break;
            case 161:
                setScanType("Arrived at Port");
                break;
            case 164:
                setScanType("Flight Departure");
                break;
            case 168:
                setScanType("Flight Arrived");
                break;
            case 200:
            case 232:
                setScanType("Customs Clearance Completed");
                break;
            case 210:
                setScanType("End Received");
                break;
        }
    }
}
