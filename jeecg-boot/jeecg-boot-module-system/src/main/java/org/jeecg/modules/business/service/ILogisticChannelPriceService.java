package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.LogisticChannelPrice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;

/**
 * @Description: 物流渠道价格
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
public interface ILogisticChannelPriceService extends IService<LogisticChannelPrice> {

	public List<LogisticChannelPrice> selectByMainId(String mainId);

    List<String> getAllCountry();
}
