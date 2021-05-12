package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.ShippingDiscount;
import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.entity.SkuPrice;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.ShippingDiscountMapper;
import org.jeecg.modules.business.mapper.SkuMapper;
import org.jeecg.modules.business.mapper.SkuPriceMapper;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.ISkuService;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.jeecg.modules.business.vo.inventory.InventoryRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: SKU表
 * @Author: jeecg-boot
 * @Date: 2021-04-01
 * @Version: V1.0
 */
@Service
public class SkuServiceImpl extends ServiceImpl<SkuMapper, Sku> implements ISkuService {

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuPriceMapper skuPriceMapper;
    @Autowired
    private ShippingDiscountMapper shippingDiscountMapper;

    @Autowired
    private IClientService clientService;

    @Autowired
    private PlatformOrderContentMapper platformOrderContentMapper;

    @Override
    @Transactional
    public void saveMain(Sku sku, List<SkuPrice> skuPriceList, List<ShippingDiscount> shippingDiscountList) {
        skuMapper.insert(sku);
        if (skuPriceList != null && skuPriceList.size() > 0) {
            for (SkuPrice entity : skuPriceList) {
                //外键设置
                entity.setSkuId(sku.getId());
                skuPriceMapper.insert(entity);
            }
        }
        if (shippingDiscountList != null && shippingDiscountList.size() > 0) {
            for (ShippingDiscount entity : shippingDiscountList) {
                //外键设置
                entity.setSkuId(sku.getId());
                shippingDiscountMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void updateMain(Sku sku, List<SkuPrice> skuPriceList, List<ShippingDiscount> shippingDiscountList) {
        skuMapper.updateById(sku);

        //1.先删除子表数据
        skuPriceMapper.deleteByMainId(sku.getId());
        shippingDiscountMapper.deleteByMainId(sku.getId());

        //2.子表数据重新插入
        if (skuPriceList != null && skuPriceList.size() > 0) {
            for (SkuPrice entity : skuPriceList) {
                //外键设置
                entity.setSkuId(sku.getId());
                skuPriceMapper.insert(entity);
            }
        }
        if (shippingDiscountList != null && shippingDiscountList.size() > 0) {
            for (ShippingDiscount entity : shippingDiscountList) {
                //外键设置
                entity.setSkuId(sku.getId());
                shippingDiscountMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void delMain(String id) {
        skuPriceMapper.deleteByMainId(id);
        shippingDiscountMapper.deleteByMainId(id);
        skuMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            skuPriceMapper.deleteByMainId(id.toString());
            shippingDiscountMapper.deleteByMainId(id.toString());
            skuMapper.deleteById(id);
        }
    }

    @Override
    public List<Sku> selectByMainId(String mainId) {
        return skuMapper.selectByMainId(mainId);
    }

    @Override
    public void fillPageForCurrentClient(Page<InventoryRecord> page) {
        // search client id for current user
        Client client = clientService.getCurrentClient();
        // in case of other roles
        if (null == client) {
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
        } else {
            String clientId = client.getId();
            List<InventoryRecord> orders = skuMapper.pageSkuByClientId(clientId, page.offset(), page.getSize());
            page.setRecords(orders);
            page.setTotal(skuMapper.countTotal(clientId));
        }
    }

    @Override
    public void addInventory(List<SkuQuantity> skuQuantities) {
        addInventory(skuQuantities,Collections.emptyList());
    }


    @Override
    public void addInventory(List<SkuQuantity> skuQuantities, List<String> platformOrderIDs) {
        Objects.requireNonNull(skuQuantities);
        Objects.requireNonNull(platformOrderIDs);

        Map<String, Integer> quantityPurchased = skuQuantities.stream()
                .collect(
                        Collectors.toMap(
                                SkuQuantity::getID,
                                SkuQuantity::getQuantity
                        )
                );

        if(!platformOrderIDs.isEmpty()){
            List<SkuQuantity> used = platformOrderContentMapper.searchOrderContent(platformOrderIDs);
            for(SkuQuantity sq : used){
                int quantity = quantityPurchased.get(sq.getID());
                quantityPurchased.put(sq.getID(), quantity-sq.getQuantity());
            }
        }

        skuMapper.addSkuQuantity(quantityPurchased);
    }
}
