package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.mapper.ClientUserMapper;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.ClientInfo;
import org.jeecg.modules.business.vo.clientPlatformOrder.ClientPlatformOrderPage;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;
import org.jeecg.modules.business.vo.clientPlatformOrder.PurchaseConfirmation;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Collection;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Service
public class PlatformOrderServiceImpl extends ServiceImpl<PlatformOrderMapper, PlatformOrder> implements IPlatformOrderService {

    private final PlatformOrderMapper platformOrderMap;
    private final PlatformOrderContentMapper platformOrderContentMap;
    private final ClientUserMapper clientUserMap;

    @Autowired
    public PlatformOrderServiceImpl(PlatformOrderMapper platformOrderMap, PlatformOrderContentMapper platformOrderContentMap, ClientUserMapper clientUserMap) {
        this.platformOrderMap = platformOrderMap;
        this.platformOrderContentMap = platformOrderContentMap;
        this.clientUserMap = clientUserMap;
    }

    @Override
    @Transactional
    public void saveMain(PlatformOrder platformOrder, List<PlatformOrderContent> platformOrderContentList) {
        platformOrderMap.insert(platformOrder);
        if (platformOrderContentList != null && platformOrderContentList.size() > 0) {
            for (PlatformOrderContent entity : platformOrderContentList) {
                //外键设置
                entity.setStatus(platformOrder.getStatus());
                entity.setPlatformOrderId(platformOrder.getId());
                platformOrderContentMap.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void updateMain(PlatformOrder platformOrder, List<PlatformOrderContent> platformOrderContentList) {
        platformOrderMap.updateById(platformOrder);

        //1.先删除子表数据
        platformOrderContentMap.deleteByMainId(platformOrder.getId());

        //2.子表数据重新插入
        if (platformOrderContentList != null && platformOrderContentList.size() > 0) {
            for (PlatformOrderContent entity : platformOrderContentList) {
                //外键设置
                entity.setStatus(platformOrder.getStatus());
                platformOrderContentMap.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void delMain(String id) {
        platformOrderContentMap.deleteByMainId(id);
        platformOrderMap.deleteById(id);
    }

    @Override
    @Transactional
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            platformOrderContentMap.deleteByMainId(id.toString());
            platformOrderMap.deleteById(id);
        }
    }

    @Override
    public void initPlatformOrderPage(IPage<ClientPlatformOrderPage> page) {
        // search client id for current user
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        Client client = clientUserMap.selectClientByUserId(sysUser.getId());
        // in case of other roles
        if (null == client) {
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
        } else {
            String clientId = client.getId();
            List<ClientPlatformOrderPage> orders = platformOrderMap.pageByClientId(clientId, page.offset(), page.getSize());
            page.setRecords(orders);
            page.setTotal(platformOrderMap.countTotal(clientId));
        }
    }

    @Override
    public OrdersStatisticData getPlatformOrdersStatisticData(List<String> orderIds) {
        List<OrderContentDetail> data = platformOrderContentMap.searchOrderContentDetail(orderIds);
        return OrdersStatisticData.makeData(data);
    }

    @Override
    public List<PlatformOrderContent> selectByMainId(String mainId) {
        return platformOrderContentMap.selectByMainId(mainId);
    }

    @Override
    public PurchaseConfirmation purchaseOrder(List<String> orderIds) {
        // get client info
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        Client client = clientUserMap.selectClientByUserId(sysUser.getId());
        ClientInfo clientInfo = new ClientInfo(client);
        List<OrderContentDetail> data;
        if (orderIds.isEmpty()) {
            data = Collections.emptyList();
        } else {
            data = platformOrderContentMap.searchOrderContentDetail(orderIds);
        }
        return new PurchaseConfirmation(clientInfo, data);

    }

}
