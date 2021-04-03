package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.entity.LogisticChannel;
import org.jeecg.modules.business.entity.LogisticChannelPrice;
import org.jeecg.modules.business.mapper.LogisticChannelPriceMapper;
import org.jeecg.modules.business.mapper.LogisticChannelMapper;
import org.jeecg.modules.business.service.ILogisticChannelService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.io.Serializable;
import java.util.List;
import java.util.Collection;

/**
 * @Description: 物流渠道
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
@Service
public class LogisticChannelServiceImpl extends ServiceImpl<LogisticChannelMapper, LogisticChannel> implements ILogisticChannelService {

	@Autowired
	private LogisticChannelMapper logisticChannelMapper;
	@Autowired
	private LogisticChannelPriceMapper logisticChannelPriceMapper;
	
	@Override
	@Transactional
	public void saveMain(LogisticChannel logisticChannel, List<LogisticChannelPrice> logisticChannelPriceList) {
		logisticChannelMapper.insert(logisticChannel);
		if(logisticChannelPriceList!=null && logisticChannelPriceList.size()>0) {
			for(LogisticChannelPrice entity:logisticChannelPriceList) {
				//外键设置
				entity.setChannelId(logisticChannel.getId());
				logisticChannelPriceMapper.insert(entity);
			}
		}
	}

	@Override
	@Transactional
	public void updateMain(LogisticChannel logisticChannel,List<LogisticChannelPrice> logisticChannelPriceList) {
		logisticChannelMapper.updateById(logisticChannel);
		
		//1.先删除子表数据
		logisticChannelPriceMapper.deleteByMainId(logisticChannel.getId());
		
		//2.子表数据重新插入
		if(logisticChannelPriceList!=null && logisticChannelPriceList.size()>0) {
			for(LogisticChannelPrice entity:logisticChannelPriceList) {
				//外键设置
				entity.setChannelId(logisticChannel.getId());
				logisticChannelPriceMapper.insert(entity);
			}
		}
	}

	@Override
	@Transactional
	public void delMain(String id) {
		logisticChannelPriceMapper.deleteByMainId(id);
		logisticChannelMapper.deleteById(id);
	}

	@Override
	@Transactional
	public void delBatchMain(Collection<? extends Serializable> idList) {
		for(Serializable id:idList) {
			logisticChannelPriceMapper.deleteByMainId(id.toString());
			logisticChannelMapper.deleteById(id);
		}
	}
	
}
