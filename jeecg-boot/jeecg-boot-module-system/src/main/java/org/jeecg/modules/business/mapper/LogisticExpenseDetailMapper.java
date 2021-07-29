package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.entity.LogisticExpenseDetail;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 物流开销明细
 * @Author: jeecg-boot
 * @Date: 2021-07-22
 * @Version: V1.0
 */
@Repository
public interface LogisticExpenseDetailMapper extends BaseMapper<LogisticExpenseDetail> {

    /**
     * Find expense details that correponds to platform orders
     *
     * @param ids serial ids
     * @return list of expense details
     */
    List<LogisticExpenseDetail> findBy(@Param("ids") Collection<String> ids);

    List<BigDecimal> findExpenseByInvoiceCodes(@Param("codes") Collection<String> codes);

}
