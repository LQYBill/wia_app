package org.jeecg.modules.business.service.impl.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.mapper.PurchaseOrderContentMapper;
import org.jeecg.modules.business.mapper.PurchaseOrderMapper;
import org.jeecg.modules.business.mapper.SkuPromotionHistoryMapper;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.service.IPurchaseOrderService;
import org.jeecg.modules.business.service.domain.codeGenerationRule.PurchaseInvoiceCodeRule;
import org.jeecg.modules.business.vo.OrderContentEntry;
import org.jeecg.modules.business.vo.PromotionHistoryEntry;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;
import org.jeecg.modules.message.handle.enums.SendMsgTypeEnum;
import org.jeecg.modules.message.util.PushMsgUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final IPlatformOrderService platformOrderService;

    private final PlatformOrderMapper platformOrderMapper;

    /**
     * Directory where payment documents are put
     */
    @Value("${jeecg.path.save}")
    private String PAYMENT_DOC_DIR;

    @Autowired
    private PushMsgUtil pushMsgUtil;

    public PurchaseOrderServiceImpl(PurchaseOrderMapper purchaseOrderMapper,
                                    PurchaseOrderContentMapper purchaseOrderContentMapper,
                                    SkuPromotionHistoryMapper skuPromotionHistoryMapper, IClientService clientService, IPlatformOrderService platformOrderService, PlatformOrderMapper platformOrderMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderContentMapper = purchaseOrderContentMapper;
        this.skuPromotionHistoryMapper = skuPromotionHistoryMapper;
        this.clientService = clientService;
        this.platformOrderService = platformOrderService;
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

    @Transactional
    @Override
    public void confirmPayment(String purchaseID) {
        // update data in DB
        purchaseOrderMapper.confirmPayment(purchaseID);
        // send email to buyer
        Map<String, String> map = new HashMap<>();
        PurchaseOrder purchaseOrder = getById(purchaseID);
        Client client = clientService.getById(purchaseOrder.getClientId());
        map.put("client_name", client.fullName());
        String invoiceNumber = purchaseOrderMapper.getInvoiceNumber(purchaseID);
        map.put("order_number", invoiceNumber);
        pushMsgUtil.sendMessage(
                SendMsgTypeEnum.EMAIL.getType(),
                "purchase_to_process",
                map,
                "service@wia-sourcing.com"
        );
    }

    @Transactional
    @Override
    public void confirmPurchase(String purchaseID) {
        // update data in DB
        purchaseOrderMapper.confirmPurchase(purchaseID);
        // send email to client
        PurchaseOrder purchaseOrder = getById(purchaseID);
        Client client = clientService.getById(purchaseOrder.getClientId());
        String invoiceNumber = purchaseOrderMapper.getInvoiceNumber(purchaseID);
        Map<String, String> map = new HashMap<>();
        map.put("client", client.getFirstName());
        map.put("order_number", invoiceNumber);
        pushMsgUtil.sendMessage(
                SendMsgTypeEnum.EMAIL.getType(),
                "purchase_order_processed",
                map,
                client.getEmail()
        );
    }

    /**
     * Generated a purchase order by series of platform orders indicated by their identifier.
     *
     * @param SkuQuantity a list of platform orders
     * @return the purchase order's identifier (UUID)
     */
    @Override
    @Transactional
    public String addPurchase(List<SkuQuantity> SkuQuantity, List<String> orderIDs) {
        Client client = clientService.getCurrentClient();

        List<OrderContentDetail> details = platformOrderService.searchPurchaseOrderDetail(SkuQuantity);
        OrdersStatisticData data = OrdersStatisticData.makeData(details);

        String purchaseID = UUID.randomUUID().toString();

        String lastInvoiceNumber = purchaseOrderMapper.lastInvoiceNumber();
        String invoiceNumber = new PurchaseInvoiceCodeRule().next(lastInvoiceNumber);
        // 1. save purchase itself
        purchaseOrderMapper.addPurchase(
                purchaseID,
                client.fullName(),
                client.getId(),
                data.getEstimatedTotalPrice(),
                data.getReducedAmount(),
                data.finalAmount(),
                invoiceNumber
        );

        // 2. save purchase's content
        List<OrderContentEntry> entries = details.stream()
                .map(d -> (new OrderContentEntry(d.getQuantity(), d.totalPrice(), d.getSkuDetail().getSkuId())))
                .collect(Collectors.toList());
        purchaseOrderContentMapper.addAll(client.fullName(), purchaseID, entries);

        // 3. save the application of promotion information
        List<PromotionHistoryEntry> promotionHistoryEntries = details.stream()
                .filter(orderContentDetail -> orderContentDetail.getSkuDetail().getPromotion() != Promotion.ZERO_PROMOTION)
                .map(orderContentDetail -> {
                    String promotion = orderContentDetail.getSkuDetail().getPromotion().getId();
                    System.out.println(promotion);
                    int count = orderContentDetail.promotionCount();
                    return new PromotionHistoryEntry(promotion, count);
                }).collect(Collectors.toList());
        if (!promotionHistoryEntries.isEmpty()) {
            skuPromotionHistoryMapper.addAll(client.fullName(), promotionHistoryEntries, purchaseID);
        }

        // TODO use real client adresse
        // send email to client
        Map<String, String> map = new HashMap<>();
        map.put("client", client.getFirstName());
        map.put("order_number", invoiceNumber);
        pushMsgUtil.sendMessage(
                SendMsgTypeEnum.EMAIL.getType(),
                "purchase_order_confirmation",
                map,
                "Matthieu.Du@outlook.com"
        );

        // 5. update platform order status to "purchasing" (optionnel)
        if (orderIDs != null && !orderIDs.isEmpty()) {
            platformOrderMapper.batchUpdateStatus(orderIDs, PlatformOrder.Status.Purchasing.code);
        }

        // 4. return purchase id
        return purchaseID;
    }

    /**
     * Save a payment file for a purchase order in the folder indicated by constant
     * {@code PAYMENT_DOC_DIR},
     * in case that the target purchase order already has a payment file, the previous file
     * will be replaced.
     * Only the basename of the file will be saved to DB, not full path name.
     *
     * @param purchaseID the purchase's identifier
     * @param in         the payment file
     * @throws IOException if an I/O error occurs when deleting previous
     *                     file, saving current file
     */
    @Transactional
    @Override
    public void savePaymentDocumentForPurchase(String purchaseID, @NotNull MultipartFile in) throws IOException {
        // save file
        String filename = purchaseID + "_" + in.getOriginalFilename();
        Path target = Paths.get(PAYMENT_DOC_DIR, filename);
        Files.deleteIfExists(target);
        Files.copy(in.getInputStream(), target);
        purchaseOrderMapper.updatePaymentDocument(purchaseID, filename);

        // send email to accountant
        Client client = clientService.getCurrentClient();
        Map<String, String> map = new HashMap<>();
        map.put("client_name", client.fullName());
        String invoiceNumber = purchaseOrderMapper.getInvoiceNumber(purchaseID);
        map.put("order_number", invoiceNumber);
        map.put("accountant", "the real account name");
        pushMsgUtil.sendMessage(
                SendMsgTypeEnum.EMAIL.getType(),
                "payment_proof_upload",
                map,
                "service@wia-sourcing.com"
        );
    }

    /**
     * Download the payment file of the purchase order by its basename
     *
     * @param filename the basename of the file
     * @return byte array of the file
     * @throws IOException IO error while reading the file.
     */
    @Override
    public byte[] downloadPaymentDocumentOfPurchase(String filename) throws IOException {
        Path target = Paths.get(PAYMENT_DOC_DIR, filename);
        return Files.readAllBytes(target);
    }
}
