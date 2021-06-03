package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.ShippingFeesWaiverProduct;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: 采购运费免除产品
 * @Author: jeecg-boot
 * @Date:   2021-06-02
 * @Version: V1.0
 */
public interface IShippingFeesWaiverProductService extends IService<ShippingFeesWaiverProduct> {

	public List<ShippingFeesWaiverProduct> selectByMainId(String mainId);
}
