package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.domain.jtapi.JTParcelTrace;
import org.jeecg.modules.business.domain.jtapi.JTParcelTraceDetail;
import org.jeecg.modules.business.entity.ParcelTrace;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description: 包裹轨迹
 * @Author: jeecg-boot
 * @Date: 2022-02-18
 * @Version: V1.0
 */
@Repository
public interface ParcelTraceMapper extends BaseMapper<ParcelTrace> {

    boolean deleteByMainId(@Param("mainId") String mainId);

    List<ParcelTrace> selectByMainId(@Param("mainId") String mainId);

    void insertOrIgnore(@Param("traces") List<JTParcelTraceDetail> traceDetails);
}
