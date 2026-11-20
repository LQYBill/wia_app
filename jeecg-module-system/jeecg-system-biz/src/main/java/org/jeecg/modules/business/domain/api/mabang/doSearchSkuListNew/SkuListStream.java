package org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.NetworkDataStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class provide stream of order.
 */
@Slf4j
public class SkuListStream implements NetworkDataStream<SkuData> {

    private final NetworkDataStream<SkuListResponse> rawStream;

    private List<SkuData> skus;

    private int index;

    private boolean began;

    /** True after the source ends or returns a successful empty final page. */
    private boolean exhausted;


    public SkuListStream(NetworkDataStream<SkuListResponse> rawStream) {
        this.rawStream = rawStream;
        skus = Collections.emptyList();
        this.index = 0;
        this.exhausted = false;
        this.began = false;
    }
    @Override
    public List<SkuData> all() {
        SkuData firstElement = attempt();
        if (firstElement == null) {
            return Collections.emptyList();
        }

        ArrayList<SkuData> res = new ArrayList<>();
        if (shouldSync(firstElement)) {
            res.add(firstElement);
        }
        while (hasNext()) {
            SkuData nextSku = next();
            if (shouldSync(nextSku)) {
                res.add(nextSku);
            }
        }
        return res;
    }

    protected boolean shouldSync(SkuData sku) {
        return SkuStatus.Normal.equals(sku.getStatus());
    }
    @Override
    public SkuData attempt() {
        began = true;
        skus = Collections.emptyList();
        index = 0;
        exhausted = false;
        log.info("Attempting for the first request");
        SkuListResponse response = rawStream.attempt();
        if (response == null) {
            exhausted = true;
            log.info("No response");
            return null;
        }
        if (response.getData() == null || response.getData().isEmpty()) {
            exhausted = true;
            log.info("Response with empty data");
            return null;
        }
        skus = response.getData().toJavaList(SkuData.class);
        index = 1;
        log.info("Returned the first element");
        return skus.get(0);
    }

    @Override
    public boolean hasNext() {
        // the first time
        if (!began) {
            throw new IllegalStateException("Calling hasNext before begin");
        }

        if (exhausted) return false;
        if (index < skus.size()) return true;
        if (!rawStream.hasNext()) {
            exhausted = true;
            return false;
        }
        // Fetch before promising another item: the cursor may lead to an empty final page.
        SkuListResponse response = rawStream.next();
        if (response == null) throw new IllegalStateException("Mabang SKU lookup unavailable");
        if (response.getData() == null || response.getData().isEmpty()) {
            exhausted = true;
            return false;
        }
        skus = response.getData().toJavaList(SkuData.class);
        index = 0;
        return true;
    }

    @Override
    public SkuData next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Stream is empty!");
        }
        log.debug("Return data at {}", index);
        SkuData res = skus.get(index);
        index++;
        return res;
    }
}
