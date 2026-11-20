package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.domain.api.mabang.doSearchSkuListNew.SkuData;
import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.vo.SkuIdBackfillRow;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Responsible for all mabang sku persistance operation
 */
@Repository
public interface SkuListMabangMapper extends BaseMapper<SkuData> {



    /**
     * Search skus that exist in DB.
     *
     * @param skuErpCode
     * @return
     */
    List<Sku> searchExistence(@Param("skuErpCode") Collection<String> skuErpCode);
    String searchSkuId(@Param("erpCode") String erpCode);
    String searchSensitiveAttributeId(@Param("enName") String enName);

    LocalDateTime skuIdBackfillNow();
    LocalDateTime skuIdBackfillUpper();

    List<SkuIdBackfillRow> findSkuIdBackfillBatch(@Param("cutoff") LocalDateTime cutoff,
            @Param("upper") LocalDateTime upper, @Param("afterTime") LocalDateTime afterTime,
            @Param("afterId") String afterId, @Param("limit") int limit);

    int updateSkuIdBackfillBatch(@Param("rows") List<SkuIdBackfillRow> rows,
            @Param("cutoff") LocalDateTime cutoff, @Param("upper") LocalDateTime upper);




}
