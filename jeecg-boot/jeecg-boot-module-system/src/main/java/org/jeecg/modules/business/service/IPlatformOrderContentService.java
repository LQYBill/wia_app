package org.jeecg.modules.business.service;

import org.jeecg.modules.business.entity.PlatformOrderContent;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: 平台订单内容
 * @Author: jeecg-boot
 * @Date:   2021-04-08
 * @Version: V1.0
 */
public interface IPlatformOrderContentService extends IService<PlatformOrderContent> {

	public List<PlatformOrderContent> selectByMainId(String mainId);
}
