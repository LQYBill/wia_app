package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.business.entity.PurchaseOrderSku;
import org.jeecg.modules.business.entity.SkuPromotionHistory;
import org.jeecg.modules.business.entity.PurchaseOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 商品采购订单
 * @Author: jeecg-boot
 * @Date: 2021-04-03
 * @Version: V1.0
 */
public interface IPurchaseOrderService extends IService<PurchaseOrder> {

    /**
     * 添加一对多
     */
    public void saveMain(PurchaseOrder purchaseOrder, List<PurchaseOrderSku> purchaseOrderSkuList, List<SkuPromotionHistory> skuPromotionHistoryList);

    /**
     * 修改一对多
     */
    public void updateMain(PurchaseOrder purchaseOrder, List<PurchaseOrderSku> purchaseOrderSkuList, List<SkuPromotionHistory> skuPromotionHistoryList);

    /**
     * 删除一对多
     */
    public void delMain(String id);

    /**
     * 批量删除一对多
     */
    public void delBatchMain(Collection<? extends Serializable> idList);

    /**
     * Set purchase orders to the page indicated by argument.
     *
     * @param page the page to put data
     */
    void setPageForCurrentClient(IPage<PurchaseOrder> page);

    /**
     * Add a new purchase by demands from client side.
     *
     * @param orderIDs a list of demands
     * @return the new purchase order identifier
     */
    String addPurchase(List<String> orderIDs);

    void savePaymentDocumentForPurchase(String purchaseID, MultipartFile in) throws IOException;

    /**
     * Download the file of the purchase order indicated by its name.
     *
     * @param filename the file's name
     * @return the file in binary
     * @throws IOException IO error while reading the file.
     */
    byte[] downloadPaymentDocumentOfPurchase(String filename) throws IOException;

}
