package org.jeecg.modules.business.mapper;

import java.util.Date;
import java.util.List;
import org.jeecg.modules.business.entity.LogisticChannelPrice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: 物流渠道价格
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
public interface LogisticChannelPriceMapper extends BaseMapper<LogisticChannelPrice> {

	public boolean deleteByMainId(@Param("mainId") String mainId);
    
	public List<LogisticChannelPrice> selectByMainId(@Param("mainId") String mainId);

	LogisticChannelPrice findLogisticsChannelPrice(String channelName, Date date, int trueWeight, String country);

	List<String> getAllCountry();
}
