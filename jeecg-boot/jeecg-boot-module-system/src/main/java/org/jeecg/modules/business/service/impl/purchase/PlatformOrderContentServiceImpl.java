package org.jeecg.modules.business.service.impl.purchase;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.entity.LogisticChannel;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.mapper.LogisticChannelMapper;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.IPlatformOrderContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlatformOrderContentServiceImpl extends ServiceImpl<PlatformOrderContentMapper, PlatformOrderContent> implements IPlatformOrderContentService {
    @Autowired
    private LogisticChannelMapper logisticChannelMapper;

    @Autowired
    private PlatformOrderContentMapper platformOrderContentMapper;

    @Autowired
    private PlatformOrderMapper platformOrderMapper;

    @Override
    public BigDecimal calculateWeight(String channelName, List<PlatformOrderContent> contentList) throws UserException {
        QueryWrapper<LogisticChannel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "zh_name", channelName);

        LogisticChannel channel = logisticChannelMapper.selectOne(queryWrapper);
        if (channel == null) {
            throw new UserException("Can not find channel: " + channelName);
        }

        List<String> skuIDs = contentList.stream().map(PlatformOrderContent::getSkuId).collect(Collectors.toList());
        log.info("sku to search");
        log.info(skuIDs.toString());
        List<Map<String, Object>> colToValue = platformOrderContentMapper.searchWeightVolumes(skuIDs);
        log.info("cols");
        log.info(colToValue.toString());

        String attr = channel.getUseVolumetricWeight() == 1 ? "volume" : "height";

        Map<String, Integer> idToWeightOrVolume = colToValue.stream()
                .collect(
                        Collectors.toMap(
                                map -> (String) map.get("id"),
                                map -> (Integer) (map.get(attr) == null ? 0 : map.get(attr))
                        )
                );

        log.info(idToWeightOrVolume.toString());

        try{
            int total = contentList.stream()
                    .mapToInt(
                            content ->
                                    (content.getQuantity() * idToWeightOrVolume.get(content.getSkuId()))
                    ).sum();

            BigDecimal res = BigDecimal.valueOf(total);

            if (channel.getUseVolumetricWeight() == 1) {
                return res.divide(BigDecimal.valueOf(channel.getVolumetricWeightFactor()), RoundingMode.HALF_DOWN);
            }
            return res;
        }catch (NullPointerException e){
            throw new UserException("Can not find weight for one sku in: " +contentList.toString());
        }

    }
}
