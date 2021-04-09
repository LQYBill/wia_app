package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.ClientSku;

import java.util.List;

/**
 * @Description: 客户 登录账户 关系
 * @Author: jeecg-boot
 * @Date:   2021-04-02
 * @Version: V1.0
 */
public interface ClientUserMapper {

	String selectByUserId(@Param("userId") String userId);
}
