package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.entity.PlatformOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date:   2021-04-03
 * @Version: V1.0
 */
public interface IPlatformOrderService extends IService<PlatformOrder> {

	/**
	 * 添加一对多
	 * 
	 */
	public void saveMain(PlatformOrder platformOrder,List<PlatformOrderContent> platformOrderContentList) ;
	
	/**
	 * 修改一对多
	 * 
	 */
	public void updateMain(PlatformOrder platformOrder,List<PlatformOrderContent> platformOrderContentList);
	
	/**
	 * 删除一对多
	 */
	public void delMain (String id);
	
	/**
	 * 批量删除一对多
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);
	
}
