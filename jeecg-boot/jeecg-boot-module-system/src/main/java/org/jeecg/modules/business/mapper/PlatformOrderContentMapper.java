package org.jeecg.modules.business.mapper;

import java.util.List;

import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

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

    /**
     * Search quantity, price and its promotion of sku which are contained in the orders
     * indicated by the identifiers.
     * <p>
     * Attention, the implementation of this method uses WHERE IN clause with foreach of mybatis, in case of empty list as argument,
     * this methods will crush.
     *
     * @param orderIdList a list of order identifiers that should not be empty.
     * @return the data
     * @throws RuntimeException in case of empty list as argument
     */
    List<OrderContentDetail> searchOrderContentDetail(List<String> orderIdList);

}
