package org.jeecg.modules.business.service.impl.purchase;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.IPlatformOrderContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PlatformOrderContentServiceImpl extends ServiceImpl<PlatformOrderContentMapper, PlatformOrderContent> implements IPlatformOrderContentService {
    @Autowired
    private PlatformOrderContentMapper platformOrderContentMapper;

    @Autowired
    private PlatformOrderMapper platformOrderMapper;

    @Override
    public BigDecimal calculateWeight(List<PlatformOrderContent> contentList) {
        return null;
    }
}
