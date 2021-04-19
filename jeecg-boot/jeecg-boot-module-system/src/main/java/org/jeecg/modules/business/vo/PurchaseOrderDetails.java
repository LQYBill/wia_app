package org.jeecg.modules.business.vo;

import lombok.Data;
import org.jeecg.modules.business.entity.OrderContentDetail;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class PurchaseOrderDetails {
    private final ClientInfo clientInfo;

    private final OrdersStatisticData data;

    private final List<VoPurchaseDetail> voPurchaseDetails;

    public PurchaseOrderDetails(ClientInfo clientInfo, List<OrderContentDetail> details) {
        this.clientInfo = clientInfo;
        this.data = OrdersStatisticData.makeData(details);
        this.voPurchaseDetails = details.stream().map(
                d -> (new VoPurchaseDetail(d.getErpCode(), d.getProduct(), d.getQuantity(), d.totalPrice()))
        ).collect(Collectors.toList());
    }
}
