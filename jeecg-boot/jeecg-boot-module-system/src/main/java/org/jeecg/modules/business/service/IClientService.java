package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.ClientSku;
import org.jeecg.modules.business.entity.Client;
import com.baomidou.mybatisplus.extension.service.IService;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 客户
 * @Author: jeecg-boot
 * @Date:   2021-04-01
 * @Version: V1.0
 */
public interface IClientService extends IService<Client> {

	/**
	 * 添加一对多
	 * 
	 */
	public void saveMain(Client client,List<ClientSku> clientSkuList) ;
	
	/**
	 * 修改一对多
	 * 
	 */
	public void updateMain(Client client,List<ClientSku> clientSkuList);
	
	/**
	 * 删除一对多
	 */
	public void delMain (String id);
	
	/**
	 * 批量删除一对多
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);
	
}
