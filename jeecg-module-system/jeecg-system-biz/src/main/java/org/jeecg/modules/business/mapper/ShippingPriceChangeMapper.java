package org.jeecg.modules.business.mapper;

import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.jeecg.modules.business.entity.LogisticChannelPrice;
import org.jeecg.modules.business.entity.ShippingPriceChangeSales;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public interface ShippingPriceChangeMapper {
    @InterceptorIgnore(tenantLine = "true")
    List<ShippingPriceChangeSales> findSales(@Param("clientId") String clientId,
                                             @Param("startDate") Date startDate);

    @InterceptorIgnore(tenantLine = "true")
    List<Map<String, Object>> findLatestWeights(@Param("skuIds") List<String> skuIds,
                                                @Param("date") Date date);

    @InterceptorIgnore(tenantLine = "true")
    List<LogisticChannelPrice> findPriceHistory(@Param("channelName") String channelName,
                                                @Param("country") String country,
                                                @Param("weight") BigDecimal weight);
}
