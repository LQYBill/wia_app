package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.entity.ClientPlatformOrderContent;
import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.jeecg.modules.business.vo.clientPlatformOrder.ClientPlatformOrderPage;
import org.jeecg.modules.business.vo.clientPlatformOrder.PurchaseConfirmation;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrderQuantity;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
public interface IPlatformOrderService extends IService<PlatformOrder> {

    /**
     * 添加一对多
     */
    void saveMain(PlatformOrder platformOrder, List<PlatformOrderContent> platformOrderContentList);

    public boolean saveBatch(Map<PlatformOrder, List<PlatformOrderContent>> orderMap);

    /**
     * 修改一对多
     */
    void updateMain(PlatformOrder platformOrder, List<PlatformOrderContent> platformOrderContentList);

    /**
     * 删除一对多
     */
    void delMain(String id);

    /**
     * 批量删除一对多
     */
    void delBatchMain(Collection<? extends Serializable> idList);

    void pendingPlatformOrderPage(IPage<ClientPlatformOrderPage> page);

    void purchasingPlatformOrderPage(IPage<ClientPlatformOrderPage> page);

    void processedPlatformOrderPage(IPage<ClientPlatformOrderPage> page);

    OrdersStatisticData getPlatformOrdersStatisticData(List<String> orderIds);

    List<PlatformOrderContent> selectByMainId(String mainId);

    List<ClientPlatformOrderContent> selectClientVersionByMainId(String mainId);

    PurchaseConfirmation confirmPurchaseByPlatformOrder(List<String> platformOrderIdList);

    PurchaseConfirmation confirmPurchaseBySkuQuantity(List<SkuQuantity> skuIDQuantityMap);

    List<OrderContentDetail> searchPurchaseOrderDetail(List<SkuQuantity> skuQuantities);

    OrderQuantity queryOrderQuantities();

    /**
     * Find all uninvoiced platform orders and its content of certains shops in a period.
     *
     * @param shopIds list of the shops
     * @param begin   begin of the period
     * @param end     end of the period
     * @return list of uninvoiced orders
     */
    Map<PlatformOrder, List<PlatformOrderContent>> findUninvoicedOrders(List<String> shopIds, Date begin, Date end);

    /**
     * Find previous invoice code
     *
     * @return previous invoice code
     */
    String findPreviousInvoice();

    /**
     * Update orders and their content to DB
     *
     * @param invoicedOrderToContent orders and their contents
     */
    void updatePlatformOrder(Map<PlatformOrder, List<PlatformOrderContent>> invoicedOrderToContent);
}