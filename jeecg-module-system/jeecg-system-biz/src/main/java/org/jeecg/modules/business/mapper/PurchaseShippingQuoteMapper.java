package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.business.entity.PurchaseShippingQuoteRecord;

public interface PurchaseShippingQuoteMapper extends BaseMapper<PurchaseShippingQuoteRecord> {
    @Select("SELECT * FROM purchase_shipping_quote WHERE id = #{id} AND client_id = #{clientId} FOR UPDATE")
    PurchaseShippingQuoteRecord lockQuote(@Param("id") String id, @Param("clientId") String clientId);

    @Select("SELECT * FROM purchase_shipping_quote WHERE purchase_order_id = #{orderId}")
    PurchaseShippingQuoteRecord findByOrder(@Param("orderId") String orderId);
}
