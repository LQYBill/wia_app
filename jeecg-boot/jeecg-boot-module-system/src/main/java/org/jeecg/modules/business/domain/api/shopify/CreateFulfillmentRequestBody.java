package org.jeecg.modules.business.domain.api.shopify;

import lombok.Getter;
import org.jeecg.modules.business.entity.PlatformOrderShopSync;

import java.math.BigInteger;

public class CreateFulfillmentRequestBody extends ShopifyRequestBody {

    public static final String ENDPOINT = "fulfillments.json";

    @Getter
    private final BigInteger fulfillmentId;

    @Getter
    private final String trackingNumber;

    public CreateFulfillmentRequestBody(String sitePrefix, BigInteger fulfillmentId, String shopToken,
                                        String trackingNumber) {
        super(sitePrefix, shopToken);
        this.fulfillmentId = fulfillmentId;
        this.trackingNumber = trackingNumber;
    }

    public CreateFulfillmentRequestBody(BigInteger fulfillmentId, PlatformOrderShopSync platformOrderShopSync) {
        this(platformOrderShopSync.getShopifyPrefix(), fulfillmentId, platformOrderShopSync.getShopifyToken(),
                platformOrderShopSync.getTrackingNumber());
    }

    @Override
    public String endpoint() {
        return ENDPOINT;
    }
}
