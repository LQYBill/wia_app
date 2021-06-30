package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.entity.PlatformOrderContent;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @Description: 平台订单内容
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
public interface IPlatformOrderContentService extends IService<PlatformOrderContent> {

    /**
     * Calculate weight of a platform order
     *
     * @param contentMap Map of <SKU ID, Quantity>
     * @return weight
     */
    BigDecimal calculateWeight(String channelName, Map<String, Integer> contentMap) throws UserException;

}
