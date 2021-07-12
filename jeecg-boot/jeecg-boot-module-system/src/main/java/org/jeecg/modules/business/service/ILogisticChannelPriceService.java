package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.entity.LogisticChannelPrice;
import org.jeecg.modules.business.vo.CountryName;
import org.jeecg.modules.business.vo.PopularCountry;

import java.util.List;

/**
 * @Description: 物流渠道价格
 * @Author: jeecg-boot
 * @Date: 2021-04-03
 * @Version: V1.0
 */
public interface ILogisticChannelPriceService extends IService<LogisticChannelPrice> {

    public List<LogisticChannelPrice> selectByMainId(String mainId);

    List<CountryName> getAllCountry();

    List<PopularCountry> getPopularCountryList();
}
