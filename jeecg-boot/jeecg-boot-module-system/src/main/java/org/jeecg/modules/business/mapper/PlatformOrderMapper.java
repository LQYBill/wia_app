package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.Order;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.vo.clientPlatformOrder.ClientPlatformOrderPage;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrderQuantity;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Repository
public interface PlatformOrderMapper extends BaseMapper<PlatformOrder> {

    List<ClientPlatformOrderPage> pagePendingOrderByClientId(@Param("clientId") String clientId, @Param("offset") long offset, @Param("size") long size);

    List<ClientPlatformOrderPage> pagePurchasingOrderByClientId(@Param("clientId") String clientId, @Param("offset") long offset, @Param("size") long size);

    List<ClientPlatformOrderPage> pageProcessedOrderByClientId(@Param("clientId") String clientId, @Param("offset") long offset, @Param("size") long size);

    /**
     * Update a platform's status.
     *
     * @param orderID identifier of the platform order to update
     * @param status  the status to set
     */
    void updateStatus(@Param("orderID") String orderID, @Param("status") int status);

    /**
     * Update a batch of platforms' status
     *
     * @param orderIDList identifiers of platform orders to update
     * @param status      the status to set
     */
    void batchUpdateStatus(@Param("orderIDList") List<String> orderIDList, @Param("status") int status);

    OrderQuantity queryQuantities(@Param("clientId") String clientId);

    /**
     * Insert platform order data from Mabang side.
     * replace shop erp code by shop id.
     *
     * @param order the order to insert
     */
    void insertFromMabangOrder(Order order);

    /**
     * Get platform order ID by erp code.
     *
     * @param platformOrderNumber erp code == platform order number
     */
    String findIdByErpCode(String platformOrderNumber);

    /**
     * Update merged order.
     * Mark in the sources their target as ID of target.
     *
     * @param targetID  ID of target platform order
     * @param sourceIDs IDs of source platform order
     */
    void updateMergedOrder(@Param("target") String targetID, @Param("sources") List<String> sourceIDs);
}
