package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.*;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.*;
import org.jeecg.modules.business.vo.inventory.InventoryRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
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
    @Autowired
    private SkuMeasureMapper skuMeasureMapper;
    @Autowired
    private SkuProductNameMapper skuProductNameMapper;
    @Autowired
    private UserSkuMapper userSkuMapper;
    @Autowired
    private PlatformOrderMapper platformOrderMapper;
    @Autowired
    private ILogisticChannelPriceService logisticChannelPriceService;
    @Autowired
    private CountryService countryService;
    @Autowired
    private IPlatformOrderContentService platformOrderContentService;

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

    @Override
    public List<SkuMeasure> measureSku(Collection<String> skuIds) {
        return skuMeasureMapper.selectBatchIds(skuIds);
    }

    @Override
    public List<SkuName> all() {
        return skuProductNameMapper.selectList(null);
    }

    @Override
    public List<UserSku> findSkuForCurrentUser() {
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = sysUser.getId();
        Map<String, Object> condition = new HashMap<>();
        condition.put("user_id", userId);
        return userSkuMapper.selectByMap(condition);
    }

    @Override
    public List<SkuChannelHistory> findHistoryBySkuId(String skuId) throws UserException {
        Map<String, BigDecimal> skuRealWeights = new HashMap<>();
        for (SkuWeightDiscount skuWeightsAndDiscount : platformOrderContentService.getAllSKUWeightsAndDiscounts()) {
            if (skuWeightsAndDiscount.getWeight() != null) {
                skuRealWeights.put(skuWeightsAndDiscount.getSkuId(),
                        skuWeightsAndDiscount.getDiscount().multiply(BigDecimal.valueOf(skuWeightsAndDiscount.getWeight())));
            }
        }
        BigDecimal skuWeight = skuRealWeights.get(skuId);

        // Find all orders containing this sku
        List<PlatformOrder> all = platformOrderMapper.findBySku(skuId);

        // group by channel name
        Map<String, List<PlatformOrder>> channelNameToOrder = all.stream()
                .collect(
                        Collectors.groupingBy(
                                PlatformOrder::getLogisticChannelName
                        )
                );

        List<SkuChannelHistory> histories = new ArrayList<>();

        // For each channel
        for (Map.Entry<String, List<PlatformOrder>> entry : channelNameToOrder.entrySet()) {
            String channelName = entry.getKey();
            // Group by country
            Map<String, List<PlatformOrder>> countryEnNameToOrder = entry.getValue().stream().collect(Collectors.groupingBy(PlatformOrder::getCountry));

            // for each country
            for (Map.Entry<String, List<PlatformOrder>> countryEntry : countryEnNameToOrder.entrySet()) {
                String countryEnName = countryEntry.getKey();
                List<PlatformOrder> orders = countryEntry.getValue();
                if (orders.isEmpty()) {
                } else if (orders.size() == 1) {
                    PlatformOrder newestOrder = orders.get(0);
                    LogisticChannelPrice price = logisticChannelPriceService.findPriceForPlatformOrder(newestOrder);
                    SkuPriceHistory now = new SkuPriceHistory(skuId, price.getEffectiveDate(), price.getRegistrationFee(), price.getCalUnitPrice().multiply(skuWeight));
                    SkuChannelHistory channelHistory = new SkuChannelHistory("", channelName, countryEnName, "", now, null);
                    histories.add(channelHistory);
                } else {
                    PlatformOrder newOrder = orders.get(0);
                    LogisticChannelPrice newPrice = logisticChannelPriceService.findPriceForPlatformOrder(newOrder);
                    SkuPriceHistory newHistory = new SkuPriceHistory(skuId, newPrice.getEffectiveDate(), newPrice.getRegistrationFee(), newPrice.getCalUnitPrice().multiply(skuWeight));

                    PlatformOrder oldOrder = orders.get(1);
                    LogisticChannelPrice oldPrice = logisticChannelPriceService.findPriceForPlatformOrder(oldOrder);
                    SkuPriceHistory oldHistory = new SkuPriceHistory(skuId, oldPrice.getEffectiveDate(), oldPrice.getRegistrationFee(), oldPrice.getCalUnitPrice().multiply(skuWeight));
                    SkuChannelHistory channelHistory = new SkuChannelHistory("", channelName, countryEnName, "", newHistory, oldHistory);
                    histories.add(channelHistory);
                }
            }
        }
        return histories;
    }
}