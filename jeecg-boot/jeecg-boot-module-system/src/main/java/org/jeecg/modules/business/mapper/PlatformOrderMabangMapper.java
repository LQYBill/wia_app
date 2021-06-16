package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.Order;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderItem;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Responsible for all mabang platform order persistance operation
 */
@Repository
public interface PlatformOrderMabangMapper extends BaseMapper<PlatformOrder> {
    /**
     * Insert platform order content from mabang side,
     * OrderItem doest not need to provide uuid.
     * sku erp code will be replaced by sku ID.
     *
     * @param order the order content to insert
     */
    void insertFromMabangOrder(@Param("order") Order order);

    /**
     * Get platform order ID by erp code.
     *
     * @param platformOrderNumber erp code == platform order number
     */
    String findIdByErpCode(String platformOrderNumber);

    String findIdByErpId(String erpId);

    /**
     * Update merged order.
     * Mark in the sources their target as ID of target.move the content of sources to target's.
     *
     * @param targetID  ID of target platform order
     * @param sourceIDs IDs of source platform order
     */
    void updateMergedOrder(@Param("target") String targetID, @Param("sources") List<String> sourceIDs);
}
