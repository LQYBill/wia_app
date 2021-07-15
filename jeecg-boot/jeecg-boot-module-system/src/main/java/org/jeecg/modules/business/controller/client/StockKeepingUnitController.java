package org.jeecg.modules.business.controller.client;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.service.ISkuService;
import org.jeecg.modules.business.vo.SkuChannelHistory;
import org.jeecg.modules.business.vo.UserSku;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "SKU client")
@RestController
@RequestMapping("/business/sku/client")
@Slf4j
public class StockKeepingUnitController {
    @Autowired
    private ISkuService skuService;

    @GetMapping(value = "/userSku")
    public Result<List<UserSku>> skuByUser() {
        List<UserSku> res = skuService.findSkuForCurrentUser();
        return Result.OK(res);
    }

    @GetMapping(value = "/channelHistory")
    public Result<?> skuChannelHistory(@RequestParam String skuId) {


//        SkuPriceHistory new1 = new SkuPriceHistory("23", new Date(), BigDecimal.valueOf(3), BigDecimal.valueOf(2));
//        SkuPriceHistory old = new SkuPriceHistory("23", new Date(), BigDecimal.valueOf(3), BigDecimal.valueOf(1.8));
//        SkuChannelHistory history1 = new SkuChannelHistory("Super fast express 1", "中欧特快专列 1", "France", "法兰西共和国", new1, old);
//        SkuChannelHistory history2 = new SkuChannelHistory("Super fast express 2", "中欧特快专列 2", "United Kingdom", "大不列颠", old, new1);
//        SkuChannelHistory history3 = new SkuChannelHistory("Super fast express 3", "中欧特快专列 3", "German", "德意志联邦共和国", old, old);

        List<SkuChannelHistory> res = null;
        try {
            res = skuService.findHistoryBySkuId(skuId);
        } catch (UserException e) {
            return Result.error(e.getLocalizedMessage());
        }

        return Result.OK(res);
    }
}
