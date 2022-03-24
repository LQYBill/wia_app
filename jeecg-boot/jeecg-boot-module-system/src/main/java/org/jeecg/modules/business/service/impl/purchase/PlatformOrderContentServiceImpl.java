package org.jeecg.modules.business.service.impl.purchase;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.entity.LogisticChannel;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.mapper.LogisticChannelMapper;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.service.IPlatformOrderContentService;
import org.jeecg.modules.business.vo.SkuWeightDiscountServiceFees;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PlatformOrderContentServiceImpl extends ServiceImpl<PlatformOrderContentMapper, PlatformOrderContent> implements IPlatformOrderContentService {
    @Autowired
    private LogisticChannelMapper logisticChannelMapper;

    @Autowired
    private PlatformOrderContentMapper platformOrderContentMapper;

    public List<SkuWeightDiscountServiceFees> getAllSKUWeightsDiscountsServiceFees() {
        return platformOrderContentMapper.getAllWeightsDiscountsServiceFees();
    }

    private LoadingCache<String, LogisticChannel> logisticChannelLoadingCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, LogisticChannel>() {
                        @Override
                        public LogisticChannel load(String channelName) {
                            QueryWrapper<LogisticChannel> queryWrapper = new QueryWrapper<>();
                            queryWrapper.eq(true, "zh_name", channelName);
                            return logisticChannelMapper.selectOne(queryWrapper);
                        }
                    });

    @Override
    public BigDecimal calculateWeight(String channelName, Map<String, Integer> contentMap,
                                      Map<String, BigDecimal> skuRealWeights) throws UserException {
        LogisticChannel channel;
        try {
            channel = logisticChannelLoadingCache.get(channelName);
        } catch (ExecutionException e) {
            String msg = "Error while retrieving logistic channel " + channelName;
            log.error(e.getMessage());
            throw new UserException(msg);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            String msg = "Found order without channel name";
            log.error(e.getMessage());
            throw new UserException(msg);
        }
        if (channel == null) {
            throw new UserException("Can not find channel: " + channelName);
        }

        List<String> skuIDs = new ArrayList<>(contentMap.keySet());
        log.info("skus : " + skuIDs);

        try {

            BigDecimal total = contentMap.entrySet().stream()
                    .map(
                            content ->
                                    (skuRealWeights.get(content.getKey()).multiply(BigDecimal.valueOf(content.getValue())))
                    ).reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("total weight : " + total);
            return total;
        } catch (NullPointerException e) {
            throw new UserException("Can not find weight for one sku in: " + contentMap);
        }

    }
}
