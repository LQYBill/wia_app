package org.jeecg.modules.business.mapper;

import java.util.List;

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

    public boolean deleteByMainId(@Param("mainId") String mainId);

    public List<PlatformOrderContent> selectByMainId(@Param("mainId") String mainId);

    /**
     * Search quantity of sku, number of types of sku and total price of the orders indicated by its identifiers.
     *
     * @param orderIds the string that can fit in WHERE IN clause
     * @return the data
     */
    OrdersStatisticData queryOrdersInfo(String orderIds);

}
