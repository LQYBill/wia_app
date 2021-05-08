package org.jeecg.modules.business.mapper;

import java.util.List;

import org.jeecg.modules.business.entity.Sku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * @Description: SKU表
 * @Author: jeecg-boot
 * @Date: 2021-04-01
 * @Version: V1.0
 */
@Repository
public interface SkuMapper extends BaseMapper<Sku> {

    boolean deleteByMainId(@Param("mainId") String mainId);

    List<Sku> selectByMainId(@Param("mainId") String mainId);


    List<Sku> pageSkuByClientId(String clientId, long offset, long size);

    long countTotal(String clientId);
}
