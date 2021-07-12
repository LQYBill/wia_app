package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Service
@Slf4j
public class PlatformOrderMabangServiceImpl extends ServiceImpl<PlatformOrderMabangMapper, Order> implements IPlatformOrderMabangService {

    private final PlatformOrderMabangMapper platformOrderMabangMapper;

    @Autowired
    public PlatformOrderMabangServiceImpl(PlatformOrderMabangMapper platformOrderMabangMapper) {
        this.platformOrderMabangMapper = platformOrderMabangMapper;
    }

    @Override
    @Transactional
    public void saveOrderFromMabang(List<Order> orders) {
        if(orders.isEmpty()){
            return;
        }
        // find orders that already existe in DB
        List<String> allPlatformOrderId = orders.stream()
                .map(Order::getPlatformOrderId)
                .collect(toList());
        List<Order> existedOrders = platformOrderMabangMapper.searchExistence(allPlatformOrderId);

        Map<String, Order> platformOrderIDToExistOrders = existedOrders.stream()
                .collect(
                        Collectors.toMap(
                                Order::getPlatformOrderId, Function.identity()
                        )
                );

        ArrayList<Order> newOrders = new ArrayList<>();
        ArrayList<Order> oldOrders = new ArrayList<>();
        for (Order order : orders) {
            Order db = platformOrderIDToExistOrders.get(order.getPlatformOrderId());
            if (db == null) {
                newOrders.add(order);
            } else {
                // for old orders get their id, update their attributes
                order.setId(db.getId());
                oldOrders.add(order);
            }
        }
        orders.clear();

        /* for new orders, insert them to DB and their children */
        List<OrderItem> allNewItems = prepareItems(newOrders);
        try {
            if (newOrders.size() != 0) {
                log.trace("{} orders to be inserted/updated.", orders.size());
                platformOrderMabangMapper.insertOrdersFromMabang(newOrders);
            }
            if (allNewItems.size() != 0) {
                platformOrderMabangMapper.insertOrderItemsFromMabang(allNewItems);
                log.trace("{} order items to be inserted/updated.", allNewItems.size());
            }
        } catch (RuntimeException e) {
            log.error(e.getLocalizedMessage());
        }

        /* for old orders, update them selfs and delete and reinsert their content. */
        List<OrderItem> allNewItemsOfOldItems = prepareItems(oldOrders);
        try {
            if (oldOrders.size() != 0) {
                log.trace("{} orders to be inserted/updated.", oldOrders.size());
                platformOrderMabangMapper.batchUpdateById(oldOrders);
                platformOrderMabangMapper.batchDeleteByMainID(oldOrders.stream().map(Order::getId).collect(toList()));
            }
            if (allNewItemsOfOldItems.size() != 0) {
                platformOrderMabangMapper.insertOrderItemsFromMabang(allNewItemsOfOldItems);
                log.trace("{} order items to be inserted/updated.", allNewItemsOfOldItems.size());
            }
        } catch (RuntimeException e) {
            log.error(e.getLocalizedMessage());
        }


    }

    private List<OrderItem> prepareItems(ArrayList<Order> oldOrders) {
        List<OrderItem> allNewItemsOfOldItems = new ArrayList<>();
        for (Order order : oldOrders) {
            order.resolveStatus();
            order.getOrderItems().forEach(
                    item -> {
                        item.setPlatformOrderId(order.getId());
                        item.setErpStatus(order.getStatus());
                    }
            );
            allNewItemsOfOldItems.addAll(order.getOrderItems());
        }
        return allNewItemsOfOldItems;
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

    private void updateExistedOrders(List<Order> orders){

    }
}
