package org.jeecg.modules.business.mapper;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingInvoiceMapper {
    long findEarliestUninvoicedPlatformOrder(List<String> shopIDs);

    long findLastestUninvoicedPlatformOrder(List<String> shopIDs);

}
