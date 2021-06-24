package org.jeecg.modules.business.mapper;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ShippingInvoiceMapper {
    Date findEarliestUninvoicedPlatformOrder(List<String> shopIDs);

    Date findLatestUninvoicedPlatformOrder(List<String> shopIDs);

}
