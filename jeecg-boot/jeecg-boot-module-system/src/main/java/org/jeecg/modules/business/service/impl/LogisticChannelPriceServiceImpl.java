package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.entity.LogisticChannelPrice;
import org.jeecg.modules.business.mapper.CountryNameMapper;
import org.jeecg.modules.business.mapper.LogisticChannelPriceMapper;
import org.jeecg.modules.business.mapper.PopularCountryMapper;
import org.jeecg.modules.business.service.ILogisticChannelPriceService;
import org.jeecg.modules.business.vo.CountryName;
import org.jeecg.modules.business.vo.PopularCountry;
import org.springframework.stereotype.Service;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: 物流渠道价格
 * @Author: William
 * @Date:   2021-06-16
 * @Version: V1.0
 */
@Service
public class LogisticChannelPriceServiceImpl extends ServiceImpl<LogisticChannelPriceMapper, LogisticChannelPrice> implements ILogisticChannelPriceService {
	
	@Autowired
	private LogisticChannelPriceMapper logisticChannelPriceMapper;

	@Autowired
	private CountryNameMapper countryNameMapper;

	@Autowired
	private PopularCountryMapper popularCountryMapper;
	
	@Override
	public List<LogisticChannelPrice> selectByMainId(String mainId) {
		return logisticChannelPriceMapper.selectByMainId(mainId);
	}

	@Override
	public List<CountryName> getAllCountry() {
		return countryNameMapper.selectList(null);
	}

	@Override
	public List<PopularCountry> getPopularCountryList() {
		return popularCountryMapper.selectList(null);
	}
}
