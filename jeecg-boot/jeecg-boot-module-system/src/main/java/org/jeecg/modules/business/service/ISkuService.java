package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.Sku;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: SKU表
 * @Author: jeecg-boot
 * @Date:   2021-04-01
 * @Version: V1.0
 */
public interface ISkuService extends IService<Sku> {

	public List<Sku> selectByMainId(String mainId);
}
