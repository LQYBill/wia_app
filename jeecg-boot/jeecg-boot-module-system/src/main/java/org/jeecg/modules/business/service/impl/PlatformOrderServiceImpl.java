package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.jeecg.modules.business.vo.clientPlatformOrder.ClientPlatformOrderPage;
import org.jeecg.modules.business.vo.clientPlatformOrder.PurchaseConfirmation;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.ClientInfo;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Service
@Slf4j
public class PlatformOrderServiceImpl extends ServiceImpl<PlatformOrderMapper, PlatformOrder> implements IPlatformOrderService {

    private final PlatformOrderMapper platformOrderMap;
    private final PlatformOrderContentMapper platformOrderContentMap;
    private final IClientService clientService;

    @Autowired
    public PlatformOrderServiceImpl(PlatformOrderMapper platformOrderMap, PlatformOrderContentMapper platformOrderContentMap, IClientService clientService) {
        this.platformOrderMap = platformOrderMap;
        this.platformOrderContentMap = platformOrderContentMap;
        this.clientService = clientService;
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
        Client client = clientService.getCurrentClient();
        // in case of other roles
        if (null == client) {
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
        } else {
            String clientId = client.getId();
            List<ClientPlatformOrderPage> orders = platformOrderMap.pagePendingOrderByClientId(clientId, page.offset(), page.getSize());
            page.setRecords(orders);
            page.setTotal(platformOrderMap.countTotal(clientId));
        }
    }

    @Override
    public OrdersStatisticData getPlatformOrdersStatisticData(List<String> orderIds) {
        List<SkuQuantity> skuIDQuantityMap = platformOrderContentMap.searchOrderContent(orderIds);
        List<OrderContentDetail> data = searchPurchaseOrderDetail(skuIDQuantityMap);
        return OrdersStatisticData.makeData(data);
    }


    @Override
    public List<PlatformOrderContent> selectByMainId(String mainId) {
        return platformOrderContentMap.selectByMainId(mainId);
    }

    @Override
    public PurchaseConfirmation confirmPurchaseByPlatformOrder(List<String> platformOrderIdList) {
        List<SkuQuantity> skuIDQuantityMap = platformOrderContentMap.searchOrderContent(platformOrderIdList);
        return confirmPurchaseBySkuQuantity(skuIDQuantityMap);
    }


    @Override
    public PurchaseConfirmation confirmPurchaseBySkuQuantity(List<SkuQuantity> skuIDQuantityMap) {
        Client client = clientService.getCurrentClient();
        ClientInfo clientInfo = new ClientInfo(client);
        return new PurchaseConfirmation(clientInfo, searchPurchaseOrderDetail(skuIDQuantityMap));
    }

    @Override
    public List<OrderContentDetail> searchPurchaseOrderDetail(List<SkuQuantity> skuQuantities) {
        // convert list of (ID, quantity) to map between ID and quantity
        Map<String, Integer> skuQuantity =
                skuQuantities.stream()
                        .collect(
                                Collectors.toMap(
                                        SkuQuantity::getID,
                                        SkuQuantity::getQuantity
                                )
                        );

        // Get list of sku ID
        List<String> skuList = skuQuantities.stream()
                .map(SkuQuantity::getID)
                .collect(Collectors.toList());

        List<OrderContentDetail> details = platformOrderContentMap.searchSkuDetail(skuList).stream()
                .map(
                        skuDetail -> new OrderContentDetail(
                                skuDetail,
                                skuQuantity.get(skuDetail.getSkuId())
                        )
                )
                .collect(Collectors.toList());
        log.info(details.toString());
        // SKU ID -> SKU detail -- (quantity) --> Order Content Detail
        return  details;
    }


}
