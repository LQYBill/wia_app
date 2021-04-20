package org.jeecg.modules.business.mapper;

import java.util.List;

import org.jeecg.modules.business.entity.SkuPromotionHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.vo.PromotionDetail;
import org.springframework.stereotype.Repository;

/**
 * @Description: SKU采购折扣历史
 * @Author: jeecg-boot
 * @Date: 2021-04-03
 * @Version: V1.0
 */
@Repository
public interface SkuPromotionHistoryMapper extends BaseMapper<SkuPromotionHistory> {

    boolean deleteByMainId(@Param("mainId") String mainId);

    List<SkuPromotionHistory> selectByMainId(@Param("mainId") String mainId);

    /**
     * Add a promotion detail to DB
     *
     */
    void add(PromotionDetail detail);

    void addAll(List<PromotionDetail> details);
}
