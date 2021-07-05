package org.jeecg.modules.business.mapper;

import org.jeecg.modules.business.entity.ShippingInvoiceEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShippingInvoiceMapper extends CrudRepository<ShippingInvoiceEntity, String> {

}
