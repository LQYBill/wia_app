package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.business.domain.jtapi.JTParcelTrace;
import org.jeecg.modules.business.entity.Parcel;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description: 包裹
 * @Author: jeecg-boot
 * @Date: 2022-02-18
 * @Version: V1.0
 */
@Repository
public interface ParcelMapper extends BaseMapper<Parcel> {

    void insertOrIgnore(List<JTParcelTrace> parcels);

    List<Parcel> searchByBillCode(List<String> billCodes);
}
