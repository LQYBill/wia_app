package org.jeecg.modules.business.domain.jtapi;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;

@Slf4j
@Data
public class JTParcelTraceDetail {

    @JSONField(deserialize = false)
    private String parcelId;

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

    /**
     * Set parcel ID, and add missing English description
     * @param parcelId Parcel ID to which the trace belongs to
     */
    public void parcelTraceProcess(String parcelId) {
        if (scanType == null) {
            return;
        }
        setParcelId(parcelId);
        if (descriptionEn == null || descriptionEn.isEmpty()) {
            switch (scanType) {
                case "Order Placed" :
                    setDescriptionEn("The order has been officially placed by the sender.");
                    break;
                case "Received by Consolidation Warehouse" :
                    setDescriptionEn("The parcel has arrived at the consolidation warehouse.");
                    break;
                case "Parcel Weighing" :
                    setDescriptionEn("The parcel is being weighed before departure.");
                    break;
                case "Bagging" :
                    setDescriptionEn("The parcel is being bagged before leaving consolidation warehouse.");
                    break;
                case "Consolidation Center Dispatch":
                    setDescriptionEn("The parcel has been dispatched from consolidation center.");
                    break;
                case "Aviation Stowage" :
                    setDescriptionEn("The parcel is being stowed into the flight." + descriptionCn);
                    break;
                case "Flight Departure" :
                    setDescriptionEn("The flight has departed.");
                    break;
                case "Flight Arrived" :
                    setDescriptionEn("The flight has arrived at its destination.");
                    break;
                case "Customs clearance" :
                    setDescriptionEn("The parcel is being cleared at the customs.");
                    break;
                case "Customs Clearance Completed" :
                    setDescriptionEn("The parcel has completed its customs clearance.");
                    break;
            }
        }
    }

}
