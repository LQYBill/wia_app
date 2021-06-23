package org.jeecg.modules.business.service.impl.purchase;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlatformOrderContentServiceImpl extends ServiceImpl<PlatformOrderContentMapper, PlatformOrderContent> implements IPlatformOrderContentService {
    private LogisticChannelMapper logisticChannelMapper;

    @Autowired
    private PlatformOrderContentMapper platformOrderContentMapper;

    @Autowired
    private PlatformOrderMapper platformOrderMapper;

    @Override
    public BigDecimal calculateWeight(String channelName, List<PlatformOrderContent> contentList) throws UserException {
        Map<String, Object> col = new HashMap<>();
        col.put("zh_name", channelName);
        List<LogisticChannel> channels = logisticChannelMapper.selectByMap(col);
        if (channels.size() != 1) {
            throw new UserException("DB has more than 1 channel named " + channelName);
        }
        LogisticChannel channel = channels.get(0);

        List<String> skuIDs = contentList.stream().map(PlatformOrderContent::getSkuId).collect(Collectors.toList());

        List<Map<String, Object>> colToValue = platformOrderContentMapper.searchWeightVolumes(skuIDs);

        String attr = channel.getUseVolumetricWeight() == 1 ? "volume" : "height";

        Map<String, Integer> idToWeightOrVolume = colToValue.stream()
                .collect(
                        Collectors.toMap(
                                map -> (String) map.get("id"),
                                map -> (Integer) map.get(attr)
                        )
                );

        int total = contentList.stream()
                .mapToInt(
                        content ->
                                (content.getQuantity() * idToWeightOrVolume.get(content.getId()))
                ).sum();

        BigDecimal res = BigDecimal.valueOf(total);

        if(channel.getUseVolumetricWeight() == 1){
            return res.divide(BigDecimal.valueOf(channel.getVolumetricWeightFactor()), RoundingMode.HALF_DOWN);
        }
        return res;
    }
}
