package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.entity.ParcelTrace;

import java.util.List;

/**
 * @Description: 包裹轨迹
 * @Author: jeecg-boot
 * @Date: 2022-02-18
 * @Version: V1.0
 */
public interface ParcelTraceMapper extends BaseMapper<ParcelTrace> {

    public boolean deleteByMainId(@Param("mainId") String mainId);

    public List<ParcelTrace> selectByMainId(@Param("mainId") String mainId);
}
