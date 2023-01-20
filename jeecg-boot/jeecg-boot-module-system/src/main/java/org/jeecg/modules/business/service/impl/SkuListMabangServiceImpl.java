package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.mabang.doSearchSkuList.SkuData;
import org.jeecg.modules.business.domain.api.mabang.doSearchSkuList.SkuListRequestErrorException;
import org.jeecg.modules.business.entity.Product;
import org.jeecg.modules.business.entity.Sku;
import org.jeecg.modules.business.mapper.SkuListMabangMapper;
import org.jeecg.modules.business.service.ISkuListMabangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class SkuListMabangServiceImpl extends ServiceImpl<SkuListMabangMapper, SkuData> implements ISkuListMabangService {

    private final SkuListMabangMapper skuListMabangMapper;

    @Autowired
    public SkuListMabangServiceImpl(SkuListMabangMapper skuListMabangMapper) {
        this.skuListMabangMapper = skuListMabangMapper;
    }

    /**
     * Save skus to DB from mabang api.
     *
     * @param skuDatas skus to save.
     */
    @Override
    @Transactional
    public void saveSkuFromMabang(List<SkuData> skuDatas) {
        if (skuDatas.isEmpty()) {
            return;
        }
        // we collect all erpCode
        List<String> allSkuErpCode = skuDatas.stream()
                .map(SkuData::getErpCode)
                .collect(toList());
        // find Skus that already exist in DB
        List<Sku> existingSkuList = skuListMabangMapper.searchExistence(allSkuErpCode);
        // We map all exisiting Skus in DB with erpCode as key
        Map<String, Sku> existingSkusIDMap = existingSkuList.stream()
                .collect(
                        Collectors.toMap(
                                Sku::getErpCode, Function.identity()
                        )
                );

        ArrayList<SkuData> newSkus = new ArrayList<>();
        for (SkuData retrievedSkuData : skuDatas) {
            Sku skuInDatabase = existingSkusIDMap.get(retrievedSkuData.getErpCode());
            // the current SkuData's erpCode is not in DB, so we add it to the list of newSkus
            if (skuInDatabase == null) {
                newSkus.add(retrievedSkuData);
            }
//            else {
//                // re-affect retrieved orders with ID in database
//                // because SkuDatas ID are automatically generated and their ID's is different from DB's ones
//                retrievedSkuData.setId(skuInDatabase.getId());
//            }
        }
        skuDatas.clear();

        /* for new skus, insert them to DB */
        try {
            if (newSkus.size() != 0) {
                // we need to check if the product exists, for that we are going to parse the Sku erpCode in to product code
                // check if the product code exists in DB, if not we create a new entry in DB and fill all the infos.
                // then we can finally add the new Sku, product has to be created first if it doesn't exist, since we need to fill productID in Sku table
                // we can now proceed to create new sku_declare_value associated with the new Sku and also sku_price
                int nbNewProducts = saveProductFromMabang(newSkus);
                log.info("{} products to be inserted/updated.", nbNewProducts);
                log.info("{} skus to be inserted/updated.", newSkus.size());


                //TODO : Inject all SkuDatas info in different tables (Sku, Sku_price, Sku_declare_value, product)
                // for product, check if magnetic or hasBattery

                //TODO : Before inserting the sku we need to get the product id
                skuListMabangMapper.insertSkusFromMabang(newSkus);
            }
        } catch (RuntimeException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    /**
     * Save products to DB from mabang api.
     *
     * @param newSkus we save the new product code of the new skus
     * @return return the number of new products to be inserted in DB
     */
    public int saveProductFromMabang(List<SkuData> newSkus) {
        List<String> allProductCodes = parseSkuListToProductCodeList(newSkus);
        // we create a product object for each new sku, so we can check if the product exists or not
        List<Product> allProducts = createProducts(newSkus);
        List< Product> existingProduct = skuListMabangMapper.searchProductExistence(allProductCodes);
        Map<String, Product> existingProductsIDMap = existingProduct.stream()
                .collect(
                        Collectors.toMap(
                                Product::getCode, Function.identity()
                        )
                );
        ArrayList<Product> newProducts = new ArrayList<>();
        for (Product retrievedProduct : allProducts) {
            Product productInDB = existingProductsIDMap.get(retrievedProduct.getCode());
            // the current product code is not in DB, so we add it to the list of newProducts
            if (productInDB == null) {
                newProducts.add(retrievedProduct);
            }
//            else {
//                retrievedProduct.setId(productInDB.getId());
//            }
        }
        skuListMabangMapper.insertProductsFromMabang(newProducts);
        return allProductCodes.size();
    }

    /**
     *  Parse the product code from Sku erpCode
     * @param skuData List of Sku erpCodes in format XXXXXXXX-XX
     * @return productCodeList in format XXXXXXXX
     * @throws SkuListRequestErrorException
     */
    public List<String> parseSkuListToProductCodeList(List<SkuData> skuData) throws SkuListRequestErrorException {
        List<String> productCodeList = new ArrayList<>();
        Pattern p = Pattern.compile("^([a-zA-Z0-9]+)-[a-zA-Z]{0,4}$");
        for(SkuData sku : skuData) {
            Matcher m = p.matcher(sku.getErpCode());
            if(m.matches()) {
                productCodeList.add(m.group(1));
            }
            else {
                throw new SkuListRequestErrorException("Erp Code : "+sku.getErpCode()+" is not a valid Sku code or is obsolete");
            }
        }
        return productCodeList;
    }

    /**
     *
     * @param skuData List of Sku erpCodes in format XXXXXXXX-XX
     * @return
     * @throws SkuListRequestErrorException
     */
    public String parseSkuToProduct(SkuData skuData) throws SkuListRequestErrorException {

        Pattern p = Pattern.compile("^([a-zA-Z0-9]+)-[a-zA-Z]{0,4}$");
        Matcher m = p.matcher(skuData.getErpCode());
        if(m.matches()) {
            return m.group(1);
        }
        else {
            throw new SkuListRequestErrorException("Erp Code : "+skuData.getErpCode()+" is not a valid Sku code or is obsolete");
        }
    }

    /**
     * Create a product object from skuDatas
     * @param skusDatas
     * @return productList the list of products created from skuDatas
     */
    public List<Product> createProducts(List<SkuData> skusDatas) {
        List<Product> productList = new ArrayList<>();
        String electric = "Electronic/Electric";
        String electroMag = "Electro-magnetic";
        for(SkuData sku : skusDatas) {
            Product p = new Product();
            p.setCode(parseSkuToProduct(sku));
            p.setZhName(sku.getNameCN());
            p.setEnName(sku.getNameEN());
            if(sku.getHasBattery() == 1) {
                if(sku.getMagnetic() == 1)
                    p.setSensitiveAttributeId(electroMag);
                else
                    p.setSensitiveAttributeId(electric);
            }
            else {
                p.setSensitiveAttributeId("");
            }
            p.setInvoiceName(sku.getNameEN());
            if(sku.getSaleRemark() != null)
                p.setWeight(parseInt(sku.getSaleRemark()));
            else
                p.setWeight(null);
            productList.add(p);
        }
        return productList;
    }
}
