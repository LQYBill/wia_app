package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.LogisticChannelPrice;
import org.jeecg.modules.business.entity.LogisticChannel;
import com.baomidou.mybatisplus.extension.service.IService;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @Description: 物流渠道
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
public interface ILogisticChannelService extends IService<LogisticChannel> {

	/**
	 * 添加一对多
	 * 
	 */
	public void saveMain(LogisticChannel logisticChannel,List<LogisticChannelPrice> logisticChannelPriceList) ;
	
	/**
	 * 修改一对多
	 * 
	 */
	public void updateMain(LogisticChannel logisticChannel,List<LogisticChannelPrice> logisticChannelPriceList);
	
	/**
	 * 删除一对多
	 */
	public void delMain (String id);
	
	/**
	 * 批量删除一对多
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);

	LogisticChannelPrice findLogisticsChannelPrice(String channelName, Date date, int trueWeight, String country);
}
