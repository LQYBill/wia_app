package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import org.jeecg.modules.business.entity.ShippingInvoiceEntity;
import org.springframework.context.ApplicationListener;
import org.springframework.data.relational.core.mapping.event.BeforeSaveEvent;

/**
 * Strategy to generate id when inserting to DB.
 */
public class BeforeSaveListener implements ApplicationListener<BeforeSaveEvent<ShippingInvoiceEntity>> {
    @Override
    public void onApplicationEvent(BeforeSaveEvent<ShippingInvoiceEntity> event) {
        ShippingInvoiceEntity entity = event.getEntity();
        /* this method is guaranteed to not be null, please omit warning of IDE in case of exist. */
        entity.setId(
                "" + new DefaultIdentifierGenerator().nextId(entity)
        );
    }
}
