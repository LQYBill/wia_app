package org.jeecg.modules.business.domain.api;

public enum ScanType {
    RETURN("Return"),
    END_ABNORMAL("End Abnormal"),
    END_DELIVERED("End Delivered"),
    END_DELIVERY("End Delivery"),
    END_RECEIVED("End Received"),
    END_ARRIVED("End Arrived"),
    FLIGHT_ARRIVED("Flight Arrived"),
    FLIGHT_DEPARTURE("Flight Departure"),
    FLIGHT_DELAY("Flight Delayed"),
    FACILITY_ARRIVED("Received by Consolidation Warehouse"),
    FACILITY_DEPARTURE("Consolidation Center Dispatch"),
    CUSTOMS_CLEARANCE_COMPLETED("Customs Clearance Completed"),
    ORDER_PLACED("Order Placed"),
    CN_CUSTOMS_INSPECTION("Customs Inspection");

    private final String desc;
    ScanType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
