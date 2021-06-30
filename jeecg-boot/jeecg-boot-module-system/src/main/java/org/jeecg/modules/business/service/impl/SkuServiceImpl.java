package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.*;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.ISkuService;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.jeecg.modules.business.vo.StockUpdate;
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
 * @Date: 2021-06-28
 * @Version: V1.1
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
    private SkuDeclaredValueMapper skuDeclaredValueMapper;
    @Autowired
    private IClientService clientService;
    @Autowired
    private PlatformOrderContentMapper platformOrderContentMapper;

    @Override
    @Transactional
    public void saveMain(Sku sku, List<SkuPrice> skuPriceList, List<ShippingDiscount> shippingDiscountList, List<SkuDeclaredValue> skuDeclaredValueList) {
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
        if (skuDeclaredValueList != null && skuDeclaredValueList.size() > 0) {
            for (SkuDeclaredValue entity : skuDeclaredValueList) {
                //外键设置
                entity.setSkuId(sku.getId());
                skuDeclaredValueMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void updateMain(Sku sku, List<SkuPrice> skuPriceList, List<ShippingDiscount> shippingDiscountList, List<SkuDeclaredValue> skuDeclaredValueList) {
        skuMapper.updateById(sku);

        //1.先删除子表数据
        skuPriceMapper.deleteByMainId(sku.getId());
        shippingDiscountMapper.deleteByMainId(sku.getId());
        skuDeclaredValueMapper.deleteByMainId(sku.getId());

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
        if (skuDeclaredValueList != null && skuDeclaredValueList.size() > 0) {
            for (SkuDeclaredValue entity : skuDeclaredValueList) {
                //外键设置
                entity.setSkuId(sku.getId());
                skuDeclaredValueMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void delMain(String id) {
        skuPriceMapper.deleteByMainId(id);
        shippingDiscountMapper.deleteByMainId(id);
        skuDeclaredValueMapper.deleteByMainId(id);
        skuMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            skuPriceMapper.deleteByMainId(id.toString());
            shippingDiscountMapper.deleteByMainId(id.toString());
            skuDeclaredValueMapper.deleteByMainId(id.toString());
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
        String clientId = null;
        if (null != client) {
            clientId = client.getId();
        }
        List<InventoryRecord> orders = skuMapper.pageSkuByClientId(clientId, page.offset(), page.getSize());
        page.setRecords(orders);
        page.setTotal(skuMapper.countTotal(clientId));

    }

    @Override
    public void addInventory(List<SkuQuantity> skuQuantities) {
        addInventory(skuQuantities, Collections.emptyList());
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

        if (!platformOrderIDs.isEmpty()) {
            List<SkuQuantity> used = platformOrderContentMapper.searchOrderContent(platformOrderIDs);
            for (SkuQuantity sq : used) {
                int quantity = quantityPurchased.get(sq.getID());
                quantityPurchased.put(sq.getID(), quantity - sq.getQuantity());
            }
        }

        skuMapper.addSkuQuantity(quantityPurchased);
    }

    @Override
    @Transactional
    public void batchUpdateStock(List<StockUpdate> list) {
        skuMapper.batchUpdateStock(list);
    }
}
