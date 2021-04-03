package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.entity.PurchaseOrderSku;
import org.jeecg.modules.business.mapper.PurchaseOrderSkuMapper;
import org.jeecg.modules.business.service.IPurchaseOrderSkuService;
import org.springframework.stereotype.Service;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: 商品采购订单SKU
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
@Service
public class PurchaseOrderSkuServiceImpl extends ServiceImpl<PurchaseOrderSkuMapper, PurchaseOrderSku> implements IPurchaseOrderSkuService {
	
	@Autowired
	private PurchaseOrderSkuMapper purchaseOrderSkuMapper;
	
	@Override
	public List<PurchaseOrderSku> selectByMainId(String mainId) {
		return purchaseOrderSkuMapper.selectByMainId(mainId);
	}
}
