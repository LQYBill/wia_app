package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.mapper.SkuMapper;
import org.jeecg.modules.business.service.ISkuService;
import org.springframework.stereotype.Service;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: SKU表
 * @Author: jeecg-boot
 * @Date:   2021-04-01
 * @Version: V1.0
 */
@Service
public class SkuServiceImpl extends ServiceImpl<SkuMapper, Sku> implements ISkuService {
	
	@Autowired
	private SkuMapper skuMapper;
	
	@Override
	public List<Sku> selectByMainId(String mainId) {
		return skuMapper.selectByMainId(mainId);
	}
}
