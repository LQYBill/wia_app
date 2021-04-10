package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.vo.OrdersStatisticInfo;
import org.jeecg.modules.business.vo.PlatformOrderPage;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date:   2021-04-08
 * @Version: V1.0
 */
public interface IClientPlatformOrderService extends IService<PlatformOrder> {

	List<PlatformOrderPage> getPlatformOrderList();

	OrdersStatisticInfo getPlatformOrdersStatisticInfo(List<String> orderIds);
}
