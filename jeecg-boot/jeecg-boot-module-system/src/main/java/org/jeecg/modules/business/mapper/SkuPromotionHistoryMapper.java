package org.jeecg.modules.business.mapper;

import java.util.List;
import org.jeecg.modules.business.entity.SkuPromotionHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: SKU采购折扣历史
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
public interface SkuPromotionHistoryMapper extends BaseMapper<SkuPromotionHistory> {

	public boolean deleteByMainId(@Param("mainId") String mainId);
    
	public List<SkuPromotionHistory> selectByMainId(@Param("mainId") String mainId);
}
