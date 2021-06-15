package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderItem;
import org.jeecg.modules.business.entity.ClientPlatformOrderContent;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.vo.SkuDetail;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description: 平台订单内容
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Repository
public interface PlatformOrderContentMapper extends BaseMapper<PlatformOrderContent> {

    boolean deleteByMainId(@Param("mainId") String mainId);

    List<PlatformOrderContent> selectByMainId(@Param("mainId") String mainId);

    List<ClientPlatformOrderContent> selectClientVersionByMainId(@Param("mainId") String mainId);


    /**
     * Search order contents of a list of order
     *
     * @param orderIDList list of identifiers of orders
     * @return map of sku ID and its quantity
     */
    List<SkuQuantity> searchOrderContent(List<String> orderIDList);


    List<SkuDetail> searchSkuDetail(List<String> skuIDs);


    /**
     * Insert platform order content from mabang side,
     * OrderItem doest not need to provide uuid.
     * sku erp code will be replaced to sku id.
     *
     * @param item the order content to insert
     */
    void insertFromMabangOrder(OrderItem item);

    /**
     * Update merged order content, move the content of sources to target's.
     *
     * @param targetID  ID of target platform order
     * @param sourceIDs IDs of sources platform order
     */
    void updateMergedOrder(@Param("target") String targetID, @Param("sources") List<String> sourceIDs);



}
