package org.jeecg.modules.business.domain.api.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WSTrackResponse {
    private Boolean ack;
    private String message;
    private List<TrackData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackData {
        private String trackingNumber;
        private String referenceNumber;
        private String consigneeCountry;
        private String product_id;
        private String trackContent;
        private String trackDate;
        private String trackLocation;
        private Integer track_status;
        private List<TrackDetail> trackDetails;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackDetail {
        private String track_city;
        private String track_content;
        private String track_date;
        private Integer track_status;
        private String track_status_code;
    }
}
