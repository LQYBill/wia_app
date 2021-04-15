package org.jeecg.modules.business.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.entity.PlatformOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date:   2021-04-08
 * @Version: V1.0
 */
public interface PlatformOrderMapper extends BaseMapper<PlatformOrder> {

    List<PlatformOrder> selectByClientId(@Param("clientId")String clientId);
}
