package org.jeecg.modules.business.service.impl;

import cn.hutool.core.util.PageUtil;
import com.alibaba.druid.sql.PagerUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.mapper.ClientUserMapper;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.IClientPlatformOrderService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.vo.PlatformOrderPage;
import org.jeecg.modules.system.util.SecurityUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Service
public class ClientPlatformOrderServiceImpl extends ServiceImpl<PlatformOrderMapper, PlatformOrder> implements IClientPlatformOrderService {

    @Autowired
    private PlatformOrderMapper platformOrderMapper;
    @Autowired
    private PlatformOrderContentMapper platformOrderContentMapper;

    @Autowired
    private ClientUserMapper clientUserMapper;

    @Override
    @Transactional
    public void saveMain(PlatformOrder platformOrder, List<PlatformOrderContent> platformOrderContentList) {
        platformOrderMapper.insert(platformOrder);
        if (platformOrderContentList != null && platformOrderContentList.size() > 0) {
            for (PlatformOrderContent entity : platformOrderContentList) {
                //外键设置
                entity.setStatus(platformOrder.getStatus());
                platformOrderContentMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void updateMain(PlatformOrder platformOrder, List<PlatformOrderContent> platformOrderContentList) {
        platformOrderMapper.updateById(platformOrder);

        //1.先删除子表数据
        platformOrderContentMapper.deleteByMainId(platformOrder.getId());

        //2.子表数据重新插入
        if (platformOrderContentList != null && platformOrderContentList.size() > 0) {
            for (PlatformOrderContent entity : platformOrderContentList) {
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
        for (Serializable id : idList) {
            platformOrderContentMapper.deleteByMainId(id.toString());
            platformOrderMapper.deleteById(id);
        }
    }

    public List<PlatformOrderPage> getPlatformOrderList() {
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String clientId = clientUserMapper.selectByUserId(sysUser.getId());
        if (null == clientId){
            return Collections.emptyList();
        }
        List<PlatformOrder> orders = platformOrderMapper.selectByClientId(clientId);
        return orders.stream().map(
                o -> {
                    PlatformOrderPage vo = new PlatformOrderPage();
                    BeanUtils.copyProperties(o, vo);
                    vo.setPlatformOrderContentList(
                            platformOrderContentMapper.selectByMainId(o.getPlatformOrderId())
                    );
                    return vo;
                }
        ).collect(Collectors.toList());
    }
}
