package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.entity.PlatformOrderContent;

import java.util.Map;

/**
 * @Description: 平台订单内容
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
public interface IPlatformOrderContentService extends IService<PlatformOrderContent> {
    /**
     * Determine whether the platform order indicated by platform order number
     * has less content than the one in the argument.
     *
     * @param platformOrderNumber  the erp code of the platform order.
     * @param platformOrderContent content of order, map between sku code and quantity
     * @return true if the data in DB has less content, otherwise false.
     */
    boolean hasMoreContent(String platformOrderNumber, Map<String, Integer> platformOrderContent);

}
