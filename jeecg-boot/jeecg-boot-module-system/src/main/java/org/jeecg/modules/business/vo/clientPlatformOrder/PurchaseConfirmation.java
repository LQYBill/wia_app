package org.jeecg.modules.business.vo.clientPlatformOrder;

import lombok.Data;
import org.jeecg.modules.business.entity.OrderContentDetail;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.ClientInfo;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.PurchaseDetail;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Confirmation data set of orders
 */
@Data
public class PurchaseConfirmation {
    private final ClientInfo clientInfo;

    private final OrdersStatisticData data;

    private final List<PurchaseDetail> voPurchaseDetails;

    public PurchaseConfirmation(ClientInfo clientInfo, List<OrderContentDetail> details) {
        this.clientInfo = clientInfo;
        this.data = OrdersStatisticData.makeData(details);
        this.voPurchaseDetails = details.stream().map(
                d -> (new PurchaseDetail(d.getErpCode(), d.getProduct(), d.getQuantity(), d.totalPrice()))
        ).collect(Collectors.toList());
    }
}