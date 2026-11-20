package org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew;

import org.jeecg.modules.business.domain.api.mabang.getorderlist.NetworkDataStream;

/**
 * This class provide stream of order.
 */
public class SkuUpdateListStream extends SkuListStream {
    public SkuUpdateListStream(NetworkDataStream<SkuListResponse> rawStream) {
        super(rawStream);
    }

    @Override
    protected boolean shouldSync(SkuData sku) {
        // Tagged SKUs must be marked even when their current status is not normally synced.
        return sku.hasAutoPurchaseLabel() || SkuStatus.Normal.equals(sku.getStatus())
                || SkuStatus.StoppedSelling.equals(sku.getStatus());
    }
}
