package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.domain.api.mabang.purDoGetProvider.ProviderData;
import org.jeecg.modules.business.vo.InvoiceMetaData;

import java.util.List;
import java.util.Map;

public interface IProviderMabangService extends IService<ProviderData> {
    /**
     * Save providers to DB from mabang api.
     *
     * @param providerDataList providers to save.
     */
    void saveProviderFromMabang(List<ProviderData> providerDataList);

    void addPurchaseOrderToMabang(Map<String, Integer> skuQuantities, InvoiceMetaData metaData);
}