package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.vo.ClientPlatformOrderPage;
import org.jeecg.modules.business.vo.OrdersStatisticData;
import org.jeecg.modules.business.vo.PlatformOrderPage;

import java.util.List;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date:   2021-04-08
 * @Version: V1.0
 */
public interface IClientPlatformOrderService extends IService<PlatformOrder> {

	void initPlatformOrderPage(IPage<ClientPlatformOrderPage> page);

	OrdersStatisticData getPlatformOrdersStatisticData(List<String> orderIds);
}
