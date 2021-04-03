package org.jeecg.modules.business.mapper;

import java.util.List;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: 平台订单内容
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
public interface PlatformOrderContentMapper extends BaseMapper<PlatformOrderContent> {

	public boolean deleteByMainId(@Param("mainId") String mainId);
    
	public List<PlatformOrderContent> selectByMainId(@Param("mainId") String mainId);
}
