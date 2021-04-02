package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.ClientSku;
import org.jeecg.modules.business.mapper.ClientSkuMapper;
import org.jeecg.modules.business.mapper.ClientMapper;
import org.jeecg.modules.business.service.IClientService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.io.Serializable;
import java.util.List;
import java.util.Collection;

/**
 * @Description: 客户
 * @Author: jeecg-boot
 * @Date:   2021-04-01
 * @Version: V1.0
 */
@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements IClientService {

	@Autowired
	private ClientMapper clientMapper;
	@Autowired
	private ClientSkuMapper clientSkuMapper;
	
	@Override
	@Transactional
	public void saveMain(Client client, List<ClientSku> clientSkuList) {
		clientMapper.insert(client);
		if(clientSkuList!=null && clientSkuList.size()>0) {
			for(ClientSku entity:clientSkuList) {
				//外键设置
				entity.setSkuId(client.getId());
				clientSkuMapper.insert(entity);
			}
		}
	}

	@Override
	@Transactional
	public void updateMain(Client client,List<ClientSku> clientSkuList) {
		clientMapper.updateById(client);
		
		//1.先删除子表数据
		clientSkuMapper.deleteByMainId(client.getId());
		
		//2.子表数据重新插入
		if(clientSkuList!=null && clientSkuList.size()>0) {
			for(ClientSku entity:clientSkuList) {
				//外键设置
				entity.setSkuId(client.getId());
				clientSkuMapper.insert(entity);
			}
		}
	}

	@Override
	@Transactional
	public void delMain(String id) {
		clientSkuMapper.deleteByMainId(id);
		clientMapper.deleteById(id);
	}

	@Override
	@Transactional
	public void delBatchMain(Collection<? extends Serializable> idList) {
		for(Serializable id:idList) {
			clientSkuMapper.deleteByMainId(id.toString());
			clientMapper.deleteById(id);
		}
	}
	
}
