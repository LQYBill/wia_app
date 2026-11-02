package org.jeecg.modules.business.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WSTrackOrderMapper {
    long countBillCodes(@Param("startDate") LocalDate startDate,
                        @Param("endExclusive") LocalDate endExclusive,
                        @Param("transporters") List<String> transporters);
    List<String> findBillCodes(@Param("after") String after, @Param("limit") int limit,
                              @Param("startDate") LocalDate startDate,
                              @Param("endExclusive") LocalDate endExclusive,
                              @Param("transporters") List<String> transporters);
}
