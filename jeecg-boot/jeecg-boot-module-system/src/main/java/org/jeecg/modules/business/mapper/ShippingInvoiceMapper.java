package org.jeecg.modules.business.mapper;

import java.util.List;

public interface ShippingInvoiceMapper {
    long findEarliestUninvoicedPlatformOrder(List<String> shopIDs);

    long findLastestUninvoicedPlatformOrder(List<String> shopIDs);

}
