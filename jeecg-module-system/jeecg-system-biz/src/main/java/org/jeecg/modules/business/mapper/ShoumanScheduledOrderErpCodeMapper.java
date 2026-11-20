package org.jeecg.modules.business.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface ShoumanScheduledOrderErpCodeMapper {
    List<String> findOrderIds(@Param("beginOrderTime") Date beginOrderTime,
                              @Param("endOrderTime") Date endOrderTime,
                              @Param("shopErpCode") String shopErpCode,
                              @Param("orderNumbers") List<String> orderNumbers);
}
