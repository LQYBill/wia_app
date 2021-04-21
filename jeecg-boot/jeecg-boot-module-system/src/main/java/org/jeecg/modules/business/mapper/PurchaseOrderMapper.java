package org.jeecg.modules.business.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.entity.PurchaseOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.business.vo.PurchaseOrderPage;
import org.springframework.stereotype.Repository;

/**
 * @Description: 商品采购订单
 * @Author: jeecg-boot
 * @Date: 2021-04-03
 * @Version: V1.0
 */
@Repository
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
    List<PurchaseOrderPage> pageForClient(@Param("clientId") String clientId, int offset, int size);

    /**
     * Add a purchase recording of a client.
     *
     * @param creator     creator's name
     * @param clientID    identifier of the client
     * @param totalAmount total amount of the purchase
     * @param discount    discount amount of the purchase
     * @param finalAmount final amount of the purchase
     */
    void addPurchase(@Param("ID") String ID, @Param("creator") String creator, @Param("clientID") String clientID,
                     @Param("totalAmount") BigDecimal totalAmount, @Param("discount") BigDecimal discount,
                     @Param("finalAmount") BigDecimal finalAmount);

    /**
     * Search purchase order by client's ID.
     *
     * @param clientID identifier of client
     * @return list of purchase order
     */
    List<PurchaseOrder> pageByClientID(
            @Param("clientID") String clientID,
            @Param("offset") long offset,
            @Param("size") long size
    );

    long countTotal(@Param("clientID") String clientID);
}
