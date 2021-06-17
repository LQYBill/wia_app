package org.jeecg.modules.business.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.Order;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderItem;
import org.jeecg.modules.business.mapper.PlatformOrderMabangMapper;
import org.jeecg.modules.business.service.IPlatformOrderMabangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Service
@Slf4j
public class PlatformOrderMabangServiceImpl implements IPlatformOrderMabangService {

    private final PlatformOrderMabangMapper platformOrderMabangMapper;

    @Autowired
    public PlatformOrderMabangServiceImpl(PlatformOrderMabangMapper platformOrderMabangMapper) {
        this.platformOrderMabangMapper = platformOrderMabangMapper;
    }

    @Override
    public void saveOrderFromMabang(List<Order> orders) {
        List<OrderItem> allItems = new ArrayList<>();
        for (Order order : orders) {
            order.getOrderItems().forEach(item -> {
                item.setPlatformOrderId(order.getPlatformOrderId());
            });
            allItems.addAll(order.getOrderItems());
        }
        log.debug("{}", orders.get(1));
        platformOrderMabangMapper.insertOrdersFromMabang(orders);
        platformOrderMabangMapper.insertOrderItemsFromMabang(allItems);

    }

    @Override
    @Transactional
    public void updateMergedOrderFromMabang(Order order, Collection<String> sourceOrderErpId) {
        String targetID = platformOrderMabangMapper.findIdByErpCode(order.getPlatformOrderNumber());
        List<String> sourceIDs = sourceOrderErpId.stream()
                .map(platformOrderMabangMapper::findIdByErpId)
                .collect(toList());

        platformOrderMabangMapper.updateMergedOrder(targetID, sourceIDs);
        platformOrderMabangMapper.updateMergedOrderItems(targetID, sourceIDs);
    }
}
