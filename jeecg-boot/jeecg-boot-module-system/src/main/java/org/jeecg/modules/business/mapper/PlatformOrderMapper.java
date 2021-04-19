package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.vo.ClientPlatformOrderPage;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Repository
public interface PlatformOrderMapper extends BaseMapper<PlatformOrder> {

    List<ClientPlatformOrderPage> pageByClientId(@Param("clientId") String clientId, @Param("offset") long offset, @Param("size") long size);

    int countTotal(@Param("clientId") String clientId);
}
