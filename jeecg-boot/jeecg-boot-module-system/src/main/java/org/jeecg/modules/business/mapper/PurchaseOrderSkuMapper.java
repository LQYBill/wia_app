package org.jeecg.modules.business.mapper;

import java.util.List;
import org.jeecg.modules.business.entity.PurchaseOrderSku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: 商品采购订单SKU
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
public interface PurchaseOrderSkuMapper extends BaseMapper<PurchaseOrderSku> {

	public boolean deleteByMainId(@Param("mainId") String mainId);
    
	public List<PurchaseOrderSku> selectByMainId(@Param("mainId") String mainId);
}
