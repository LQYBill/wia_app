package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.ShippingInvoice;
import org.springframework.stereotype.Repository;

/**
 * @Description: 物流发票
 * @Author: jeecg-boot
 * @Date: 2022-12-20
 * @Version: V1.0
 */
@Repository
public interface ShippingInvoiceMapper extends BaseMapper<ShippingInvoice> {

    Client fetchShopOwnerNameFromInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);
}
