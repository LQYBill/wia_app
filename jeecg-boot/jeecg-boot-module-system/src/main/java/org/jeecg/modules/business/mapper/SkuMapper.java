package org.jeecg.modules.business.mapper;

import java.util.List;
import org.jeecg.modules.business.entity.Sku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: SKU表
 * @Author: jeecg-boot
 * @Date:   2021-04-01
 * @Version: V1.0
 */
public interface SkuMapper extends BaseMapper<Sku> {

	public boolean deleteByMainId(@Param("mainId") String mainId);
    
	public List<Sku> selectByMainId(@Param("mainId") String mainId);
}
