package org.jeecg.modules.business.service.impl.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.*;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.IPurchaseOrderService;
import org.jeecg.modules.business.service.domain.codeGenerationRule.PurchaseInvoiceCodeRule;
import org.jeecg.modules.business.vo.OrderContentEntry;
import org.jeecg.modules.business.vo.PromotionHistoryEntry;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;
import org.jeecg.modules.message.handle.enums.SendMsgTypeEnum;
import org.jeecg.modules.message.util.PushMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 商品采购订单
 * @Author: jeecg-boot
 * @Date: 2021-04-03
 * @Version: V1.0
 */
@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements IPurchaseOrderService {

    private final PurchaseOrderMapper purchaseOrderMapper;

    private final PurchaseOrderContentMapper purchaseOrderContentMapper;

    private final SkuPromotionHistoryMapper skuPromotionHistoryMapper;

    private final IClientService clientService;

    private final PlatformOrderContentMapper platformOrderContentMapper;
    private final PlatformOrderMapper platformOrderMapper;

    @Autowired
    private PushMsgUtil pushMsgUtil;

    public PurchaseOrderServiceImpl(PurchaseOrderMapper purchaseOrderMapper,
                                    PurchaseOrderContentMapper purchaseOrderContentMapper,
                                    SkuPromotionHistoryMapper skuPromotionHistoryMapper, IClientService clientService, PlatformOrderContentMapper platformOrderContentMapper, PlatformOrderMapper platformOrderMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderContentMapper = purchaseOrderContentMapper;
        this.skuPromotionHistoryMapper = skuPromotionHistoryMapper;
        this.clientService = clientService;
        this.platformOrderContentMapper = platformOrderContentMapper;
        this.platformOrderMapper = platformOrderMapper;
    }

    @Override
    @Transactional
    public void saveMain(PurchaseOrder purchaseOrder, List<PurchaseOrderSku> purchaseOrderSkuList, List<SkuPromotionHistory> skuPromotionHistoryList) {
        purchaseOrderMapper.insert(purchaseOrder);
        if (purchaseOrderSkuList != null && purchaseOrderSkuList.size() > 0) {
            for (PurchaseOrderSku entity : purchaseOrderSkuList) {
                //外键设置
                entity.setSkuId(purchaseOrder.getId());
                purchaseOrderContentMapper.insert(entity);
            }
        }
        if (skuPromotionHistoryList != null && skuPromotionHistoryList.size() > 0) {
            for (SkuPromotionHistory entity : skuPromotionHistoryList) {
                //外键设置
                entity.setPromotionId(purchaseOrder.getId());
                skuPromotionHistoryMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void updateMain(PurchaseOrder purchaseOrder, List<PurchaseOrderSku> purchaseOrderSkuList, List<SkuPromotionHistory> skuPromotionHistoryList) {
        purchaseOrderMapper.updateById(purchaseOrder);

        //1.先删除子表数据
        purchaseOrderContentMapper.deleteByMainId(purchaseOrder.getId());
        skuPromotionHistoryMapper.deleteByMainId(purchaseOrder.getId());

        //2.子表数据重新插入
        if (purchaseOrderSkuList != null && purchaseOrderSkuList.size() > 0) {
            for (PurchaseOrderSku entity : purchaseOrderSkuList) {
                //外键设置
                entity.setSkuId(purchaseOrder.getId());
                purchaseOrderContentMapper.insert(entity);
            }
        }
        if (skuPromotionHistoryList != null && skuPromotionHistoryList.size() > 0) {
            for (SkuPromotionHistory entity : skuPromotionHistoryList) {
                //外键设置
                entity.setPromotionId(purchaseOrder.getId());
                skuPromotionHistoryMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void delMain(String id) {
        purchaseOrderContentMapper.deleteByMainId(id);
        skuPromotionHistoryMapper.deleteByMainId(id);
        purchaseOrderMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            purchaseOrderContentMapper.deleteByMainId(id.toString());
            skuPromotionHistoryMapper.deleteByMainId(id.toString());
            purchaseOrderMapper.deleteById(id);
        }
    }

    public void setPageForCurrentClient(IPage<PurchaseOrder> page) {
        Client client = clientService.getCurrentClient();
        if (client == null) {
            return;
        }
        List<PurchaseOrder> purchaseOrderList = purchaseOrderMapper.pageByClientID(client.getId(), page.offset(), page.getSize());
        page.setRecords(purchaseOrderList);
        long total = purchaseOrderMapper.countTotal(client.getId());
        page.setTotal(total);
    }

    @Override
    @Transactional
    public String addPurchase(List<String> orderIDs) {
        Client client = clientService.getCurrentClient();
        List<OrderContentDetail> details = platformOrderContentMapper.searchOrderContentDetail(orderIDs);
        OrdersStatisticData data = OrdersStatisticData.makeData(details);
        String purchaseID = UUID.randomUUID().toString();

        String invoiceNumber = new PurchaseInvoiceCodeRule().next(purchaseOrderMapper.lastInvoiceNumber());
        // 1. save purchase itself
        purchaseOrderMapper.addPurchase(
                purchaseID,
                client.getFirstName() + " " + client.getSurname(),
                client.getId(),
                data.getEstimatedTotalPrice(),
                data.getReducedAmount(),
                data.getEstimatedTotalPrice().subtract(data.getReducedAmount()),
                invoiceNumber
        );

        // 2. save purchase's content
        List<OrderContentEntry> entries = details.stream()
                .map(d -> (new OrderContentEntry(d.getQuantity(), d.totalPrice(), d.getSkuId())))
                .collect(Collectors.toList());
        purchaseOrderContentMapper.addAll(client.getFullName(), purchaseID, entries);

        // 3. save the application of promotion information
        List<PromotionHistoryEntry> promotionHistoryEntries = details.stream()
                .filter(orderContentDetail -> orderContentDetail.getPromotion() != Promotion.ZERO_PROMOTION)
                .map(orderContentDetail -> {
                    String promotion = orderContentDetail.getPromotion().getId();
                    System.out.println(promotion);
                    int count = orderContentDetail.promotionCount();
                    return new PromotionHistoryEntry(promotion, count);
                }).collect(Collectors.toList());
        if (!promotionHistoryEntries.isEmpty()) {
            skuPromotionHistoryMapper.addAll(client.getFullName(), promotionHistoryEntries, purchaseID);
        }

        // send email to client
        Map<String, String> map = new HashMap<>();
        map.put("user", client.getFirstName());
        map.put("order_number", invoiceNumber);
        // TODO: 4/21/2021 change sentTo to real client email
        pushMsgUtil.sendMessage(
                SendMsgTypeEnum.EMAIL.getType(),
                "purchase_order_confirmation",
                map,
                "service@wia-sourcing.com"
        );

        // 5. update platform order status to "purchasing"
        platformOrderMapper.batchUpdateStatus(orderIDs, PlatformOrder.PURCHASING_STATUS);

        // 4. return purchase id
        return purchaseID;
    }
}
