package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.service.IPlatformOrderContentService;
import org.springframework.stereotype.Service;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: 平台订单内容
 * @Author: jeecg-boot
 * @Date:   2021-04-08
 * @Version: V1.0
 */
@Service
public class PlatformOrderContentServiceImpl extends ServiceImpl<PlatformOrderContentMapper, PlatformOrderContent> implements IPlatformOrderContentService {
	
	@Autowired
	private PlatformOrderContentMapper platformOrderContentMapper;
	
	@Override
	public List<PlatformOrderContent> selectByMainId(String mainId) {
		return platformOrderContentMapper.selectByMainId(mainId);
	}
}
