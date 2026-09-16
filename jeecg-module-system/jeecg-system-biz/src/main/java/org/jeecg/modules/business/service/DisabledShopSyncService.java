package org.jeecg.modules.business.service;

import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.business.entity.Shop;
import org.jeecg.modules.business.mapper.ClientMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DisabledShopSyncService {
    @Autowired
    private IShopService shopService;
    @Autowired
    private ClientMapper clientMapper;

    @Transactional(rollbackFor = Exception.class)
    public List<Shop> deactivate(Set<String> disabledNames) {
        if (disabledNames.isEmpty()) return java.util.Collections.emptyList();
        // Mabang shop name maps to local erp_code, not the local primary key.
        List<Shop> shops = shopService.lambdaQuery()
                .eq(Shop::getActive, "1")
                .in(Shop::getErpCode, disabledNames).list();
        Set<String> owners = new LinkedHashSet<>();
        for (Shop shop : shops) {
            shopService.lambdaUpdate()
                    .eq(Shop::getId, shop.getId())
                    .eq(Shop::getActive, "1").set(Shop::getActive, "0")
                    .set(Shop::getUpdateTime, new Date()).update();
            if (StringUtils.isNotBlank(shop.getOwnerId())) owners.add(shop.getOwnerId());
        }
        for (String owner : owners) {
            clientMapper.deactivateClientWithoutActiveShops(owner);
        }
        // Include shops already inactive so cancellation failures are retried next run.
        return shops;
    }
}