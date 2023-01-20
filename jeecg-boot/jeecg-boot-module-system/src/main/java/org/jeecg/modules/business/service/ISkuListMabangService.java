package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.business.domain.api.mabang.doSearchSkuList.SkuData;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ISkuListMabangService extends IService<SkuData> {
    /**
     * Save skus to DB from mabang api.
     *
     * @param skus skus to save.
     */
    void saveSkuFromMabang(List<SkuData> skus);

    /**
     * Save products to DB from mabang api.
     *
     * @param newSkus we save the new product code of the new skus
     * @return return the number of new products to be inserted in DB
     */
    int saveProductFromMabang(List<SkuData> newSkus);

    /**
     * Parse a Sku erpCode into a Product code
     * @param skuData Sku erpCode in format XXXXXXXX-XX
     * @return product code in format XXXXXXXX
     */
   String parseSkuToProduct(SkuData skuData) throws Exception;

    /**
     * Parse a Sku erpCode list and return a Product code list
     * @param erpCodeList List of Sku erpCode in format XXXXXXXX-XX
     * @return List of product code in format XXXXXXXX
     */
    List<String> parseSkuListToProductCodeList(List<SkuData> erpCodeList) throws Exception;
}
