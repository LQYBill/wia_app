package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.entity.LogisticChannelPrice;
import org.jeecg.modules.business.mapper.LogisticChannelPriceMapper;
import org.jeecg.modules.business.service.ILogisticChannelPriceService;
import org.springframework.stereotype.Service;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: 物流渠道价格
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
@Service
public class LogisticChannelPriceServiceImpl extends ServiceImpl<LogisticChannelPriceMapper, LogisticChannelPrice> implements ILogisticChannelPriceService {
	
	@Autowired
	private LogisticChannelPriceMapper logisticChannelPriceMapper;
	
	@Override
	public List<LogisticChannelPrice> selectByMainId(String mainId) {
		return logisticChannelPriceMapper.selectByMainId(mainId);
	}
}
