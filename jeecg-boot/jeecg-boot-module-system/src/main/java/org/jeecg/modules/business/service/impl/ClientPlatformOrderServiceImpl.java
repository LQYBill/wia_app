package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.Promotion;
import org.jeecg.modules.business.mapper.ClientUserMapper;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.mapper.PromotionMapper;
import org.jeecg.modules.business.service.IClientPlatformOrderService;
import org.jeecg.modules.business.vo.OrdersStatisticData;
import org.jeecg.modules.business.vo.PlatformOrderPage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: 平台订单表
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
    public List<PlatformOrderPage> getPlatformOrderList() {
        // search client id for current user
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String clientId = clientUserMapper.selectByUserId(sysUser.getId());
        // in case of other roles
        if (null == clientId) {
            return Collections.emptyList();
        }
        List<PlatformOrder> orders = platformOrderMapper.selectByClientId(clientId);
        return orders.stream().map(
                o -> {
                    PlatformOrderPage vo = new PlatformOrderPage();
                    BeanUtils.copyProperties(o, vo);
                    vo.setPlatformOrderContentList(
                            platformOrderContentMapper.selectByMainId(o.getPlatformOrderId())
                    );
                    return vo;
                }
        ).collect(Collectors.toList());
    }

    @Override
    public OrdersStatisticData getPlatformOrdersStatisticData(List<String> orderIds) {
        List<OrderContentDetail> data = platformOrderContentMapper.searchOrderContentDetail(orderIds);
        return OrdersStatisticData.makeData(data);
    }
}
