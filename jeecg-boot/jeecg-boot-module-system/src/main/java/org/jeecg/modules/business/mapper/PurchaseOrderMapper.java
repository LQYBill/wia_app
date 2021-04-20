package org.jeecg.modules.business.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.entity.PurchaseOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.business.vo.PurchaseOrderPage;
import org.springframework.stereotype.Repository;

/**
 * @Description: 商品采购订单
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
@Repository
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
    List<PurchaseOrderPage> pageForClient(@Param("clientId") String clientId, int offset, int size);
}
