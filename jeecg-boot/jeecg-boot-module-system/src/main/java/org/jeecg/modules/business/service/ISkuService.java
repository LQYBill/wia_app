package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.business.entity.ShippingDiscount;
import org.jeecg.modules.business.entity.Sku;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.entity.SkuPrice;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: SKU表
 * @Author: jeecg-boot
 * @Date:   2021-04-01
 * @Version: V1.0
 */
public interface ISkuService extends IService<Sku> {

	public List<Sku> selectByMainId(String mainId);

	/**
	 * 添加一对多
	 *
	 */
	public void saveMain(Sku sku,List<SkuPrice> skuPriceList,List<ShippingDiscount> shippingDiscountList) ;

	/**
	 * 修改一对多
	 *
	 */
	public void updateMain(Sku sku, List<SkuPrice> skuPriceList, List<ShippingDiscount> shippingDiscountList);

	/**
	 * 删除一对多
	 */
	public void delMain (String id);

	/**
	 * 批量删除一对多
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);

    void fillPageBySkuForCurrentClient(Page<Sku> page);
}
