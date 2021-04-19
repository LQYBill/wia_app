package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.mapper.ClientUserMapper;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.IClientPlatformOrderService;
import org.jeecg.modules.business.vo.ClientPlatformOrderPage;
import org.jeecg.modules.business.vo.OrdersStatisticData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @Description: Client platform order page service
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Service
public class ClientPlatformOrderServiceImpl extends ServiceImpl<PlatformOrderMapper, PlatformOrder> implements IClientPlatformOrderService {

    private final PlatformOrderMapper platformOrderMapper;
    private final PlatformOrderContentMapper platformOrderContentMapper;
    private final ClientUserMapper clientUserMapper;

    @Autowired
    public ClientPlatformOrderServiceImpl(PlatformOrderMapper platformOrderMapper,
                                          PlatformOrderContentMapper platformOrderContentMapper,
                                          ClientUserMapper clientUserMapper
    ) {
        this.platformOrderMapper = platformOrderMapper;
        this.platformOrderContentMapper = platformOrderContentMapper;
        this.clientUserMapper = clientUserMapper;
    }

    @Override
    public void initPlatformOrderPage(IPage<ClientPlatformOrderPage> page) {
        // search client id for current user
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String clientId = clientUserMapper.selectByUserId(sysUser.getId());
        // in case of other roles
        if (null == clientId) {
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
        } else {
            List<ClientPlatformOrderPage> orders = platformOrderMapper.pageByClientId(clientId, page.offset(), page.getSize());
            page.setRecords(orders);
            page.setTotal(platformOrderMapper.countTotal(clientId));
        }
    }

    @Override
    public OrdersStatisticData getPlatformOrdersStatisticData(List<String> orderIds) {
        List<OrderContentDetail> data = platformOrderContentMapper.searchOrderContentDetail(orderIds);
        return OrdersStatisticData.makeData(data);
    }
}
