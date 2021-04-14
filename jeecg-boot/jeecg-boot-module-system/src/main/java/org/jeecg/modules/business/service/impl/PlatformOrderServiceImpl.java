package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.io.Serializable;
import java.util.List;
import java.util.Collection;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date:   2021-04-08
 * @Version: V1.0
 */
@Service
public class PlatformOrderServiceImpl extends ServiceImpl<PlatformOrderMapper, PlatformOrder> implements IPlatformOrderService {

	@Autowired
	private PlatformOrderMapper platformOrderMapper;
	@Autowired
	private PlatformOrderContentMapper platformOrderContentMapper;
	
	@Override
	@Transactional
	public void saveMain(PlatformOrder platformOrder, List<PlatformOrderContent> platformOrderContentList) {
		platformOrderMapper.insert(platformOrder);
		if(platformOrderContentList!=null && platformOrderContentList.size()>0) {
			for(PlatformOrderContent entity:platformOrderContentList) {
				//外键设置
				entity.setStatus(platformOrder.getStatus());
				entity.setPlatformOrderId(platformOrder.getId());
				platformOrderContentMapper.insert(entity);
			}
		}
	}

	@Override
	@Transactional
	public void updateMain(PlatformOrder platformOrder,List<PlatformOrderContent> platformOrderContentList) {
		platformOrderMapper.updateById(platformOrder);
		
		//1.先删除子表数据
		platformOrderContentMapper.deleteByMainId(platformOrder.getId());
		
		//2.子表数据重新插入
		if(platformOrderContentList!=null && platformOrderContentList.size()>0) {
			for(PlatformOrderContent entity:platformOrderContentList) {
				//外键设置
				entity.setStatus(platformOrder.getStatus());
				platformOrderContentMapper.insert(entity);
			}
		}
	}

	@Override
	@Transactional
	public void delMain(String id) {
		platformOrderContentMapper.deleteByMainId(id);
		platformOrderMapper.deleteById(id);
	}

	@Override
	@Transactional
	public void delBatchMain(Collection<? extends Serializable> idList) {
		for(Serializable id:idList) {
			platformOrderContentMapper.deleteByMainId(id.toString());
			platformOrderMapper.deleteById(id);
		}
	}
	
}
