package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.entity.ShippingDiscount;
import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.entity.SkuPrice;
import org.jeecg.modules.business.mapper.ShippingDiscountMapper;
import org.jeecg.modules.business.mapper.SkuMapper;
import org.jeecg.modules.business.mapper.SkuPriceMapper;
import org.jeecg.modules.business.service.ISkuService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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

	@Autowired
	private SkuPriceMapper skuPriceMapper;
	@Autowired
	private ShippingDiscountMapper shippingDiscountMapper;

	@Override
	@Transactional
	public void saveMain(Sku sku, List<SkuPrice> skuPriceList,List<ShippingDiscount> shippingDiscountList) {
		skuMapper.insert(sku);
		if(skuPriceList!=null && skuPriceList.size()>0) {
			for(SkuPrice entity:skuPriceList) {
				//外键设置
				entity.setSkuId(sku.getId());
				skuPriceMapper.insert(entity);
			}
		}
		if(shippingDiscountList!=null && shippingDiscountList.size()>0) {
			for(ShippingDiscount entity:shippingDiscountList) {
				//外键设置
				entity.setSkuId(sku.getId());
				shippingDiscountMapper.insert(entity);
			}
		}
	}

	@Override
	@Transactional
	public void updateMain(Sku sku,List<SkuPrice> skuPriceList,List<ShippingDiscount> shippingDiscountList) {
		skuMapper.updateById(sku);

		//1.先删除子表数据
		skuPriceMapper.deleteByMainId(sku.getId());
		shippingDiscountMapper.deleteByMainId(sku.getId());

		//2.子表数据重新插入
		if(skuPriceList!=null && skuPriceList.size()>0) {
			for(SkuPrice entity:skuPriceList) {
				//外键设置
				entity.setSkuId(sku.getId());
				skuPriceMapper.insert(entity);
			}
		}
		if(shippingDiscountList!=null && shippingDiscountList.size()>0) {
			for(ShippingDiscount entity:shippingDiscountList) {
				//外键设置
				entity.setSkuId(sku.getId());
				shippingDiscountMapper.insert(entity);
			}
		}
	}

	@Override
	@Transactional
	public void delMain(String id) {
		skuPriceMapper.deleteByMainId(id);
		shippingDiscountMapper.deleteByMainId(id);
		skuMapper.deleteById(id);
	}

	@Override
	@Transactional
	public void delBatchMain(Collection<? extends Serializable> idList) {
		for(Serializable id:idList) {
			skuPriceMapper.deleteByMainId(id.toString());
			shippingDiscountMapper.deleteByMainId(id.toString());
			skuMapper.deleteById(id);
		}
	}
	@Override
	public List<Sku> selectByMainId(String mainId) {
		return skuMapper.selectByMainId(mainId);
	}
}
