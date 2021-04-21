package org.jeecg.modules.business.service.impl.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PurchaseOrderContentMapper;
import org.jeecg.modules.business.mapper.SkuPromotionHistoryMapper;
import org.jeecg.modules.business.mapper.PurchaseOrderMapper;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.IPurchaseOrderService;
import org.jeecg.modules.business.vo.OrderContentEntry;
import org.jeecg.modules.business.vo.PromotionHistoryEntry;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
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

    public PurchaseOrderServiceImpl(PurchaseOrderMapper purchaseOrderMapper,
                                    PurchaseOrderContentMapper purchaseOrderContentMapper,
                                    SkuPromotionHistoryMapper skuPromotionHistoryMapper, IClientService clientService, PlatformOrderContentMapper platformOrderContentMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderContentMapper = purchaseOrderContentMapper;
        this.skuPromotionHistoryMapper = skuPromotionHistoryMapper;
        this.clientService = clientService;
        this.platformOrderContentMapper = platformOrderContentMapper;
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
    public String addPurchase(List<String> orderIDs) {
        Client client = clientService.getCurrentClient();
        List<OrderContentDetail> details = platformOrderContentMapper.searchOrderContentDetail(orderIDs);
        OrdersStatisticData data = OrdersStatisticData.makeData(details);
        String purchaseID = UUID.randomUUID().toString();

        // 1. save purchase itself
        purchaseOrderMapper.addPurchase(
                purchaseID,
                client.getFirstName() + " " + client.getSurname(),
                client.getId(),
                data.getEstimatedTotalPrice(),
                data.getReducedAmount(),
                data.getEstimatedTotalPrice().subtract(data.getReducedAmount())
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
        JavaMailSender mailSender = (JavaMailSender) SpringContextUtils.getBean("mailSender");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Matthieu.DU@outlook.com");
        message.setTo("Matthieu.DU@outlook.com");
        message.setSubject("Test mail sending");
        message.setText("Test sending mail from system");
        mailSender.send(message);

        // 4. return purchase id
        return purchaseID;
    }
}
