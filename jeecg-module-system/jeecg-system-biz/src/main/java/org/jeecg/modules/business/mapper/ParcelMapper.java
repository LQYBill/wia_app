package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.business.domain.api.cmk.CMKParcelTraceData;
import org.jeecg.modules.business.domain.api.equick.EQuickResponse;
import org.jeecg.modules.business.domain.api.hualei.HLResponseItem;
import org.jeecg.modules.business.domain.api.jt.JTParcelTrace;
import org.jeecg.modules.business.domain.api.yd.YDTraceData;
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

    void insertOrIgnoreJTParcels(List<JTParcelTrace> parcels);

    List<Parcel> searchByBillCode(List<String> billCodes);

    void insertOrUpdateEQParcels(List<EQuickResponse> parcels);

    void insertOrIgnoreYDParcels(List<YDTraceData> parcels);
    void insertOrIgnoreCMKParcels(List<CMKParcelTraceData> parcels);
    void insertOrIgnoreHLParcels(List<HLResponseItem> parcels);

    /**
     * fetch all parcels from platform order's tracking number, to archive
     * @param trackingNumbers
     * @return List of parcels
     */
    List<Parcel> fetchParcelsToArchive(@Param("trackingNumbers") List<String> trackingNumbers);

    /**
     * Inserts into parce_trace_delete table parcel traces to archive
     * @param parcels
     */
    void insertParcelsArchive(@Param("parcels") List<Parcel> parcels);
}
