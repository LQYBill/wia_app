package org.jeecg.modules.business.mapper;

import java.math.BigDecimal;
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

	/**
	 * Find logistic channel price by indicting its channel name, and destination country,
	 * also the platform order's shipping time and weight.
	 *
	 * @param channelName  the channel name
	 * @param shippingTime the shipping time
	 * @param weight       the weight
	 * @param country      the country
	 * @return one propre price
	 */
	LogisticChannelPrice findBy(String channelName, Date shippingTime, BigDecimal weight, String country);
}
