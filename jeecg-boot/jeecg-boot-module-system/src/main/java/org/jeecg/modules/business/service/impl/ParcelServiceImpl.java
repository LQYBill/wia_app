package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.entity.Parcel;
import org.jeecg.modules.business.entity.ParcelTrace;
import org.jeecg.modules.business.mapper.ParcelMapper;
import org.jeecg.modules.business.mapper.ParcelTraceMapper;
import org.jeecg.modules.business.service.IParcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 包裹
 * @Author: jeecg-boot
 * @Date: 2022-02-18
 * @Version: V1.0
 */
@Service
@Slf4j
public class ParcelServiceImpl extends ServiceImpl<ParcelMapper, Parcel> implements IParcelService {

    @Autowired
    private ParcelMapper parcelMapper;
    @Autowired
    private ParcelTraceMapper parcelTraceMapper;

    @Override
    public void saveParcelFromJT(List<Parcel> parcels) {
        return;
    }

    @Override
    @Transactional
    public void saveMain(Parcel parcel, List<ParcelTrace> parcelTraceList) {
        parcelMapper.insert(parcel);
        if (parcelTraceList != null && parcelTraceList.size() > 0) {
            for (ParcelTrace entity : parcelTraceList) {
                //外键设置
                entity.setParcelId(parcel.getId());
                parcelTraceMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void updateMain(Parcel parcel, List<ParcelTrace> parcelTraceList) {
        parcelMapper.updateById(parcel);

        //1.先删除子表数据
        parcelTraceMapper.deleteByMainId(parcel.getId());

        //2.子表数据重新插入
        if (parcelTraceList != null && parcelTraceList.size() > 0) {
            for (ParcelTrace entity : parcelTraceList) {
                //外键设置
                entity.setParcelId(parcel.getId());
                parcelTraceMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void delMain(String id) {
        parcelTraceMapper.deleteByMainId(id);
        parcelMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            parcelTraceMapper.deleteByMainId(id.toString());
            parcelMapper.deleteById(id);
        }
    }

}
