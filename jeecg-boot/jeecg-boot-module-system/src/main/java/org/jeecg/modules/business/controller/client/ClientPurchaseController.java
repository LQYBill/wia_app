package org.jeecg.modules.business.controller.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.business.entity.PurchaseOrder;
import org.jeecg.modules.business.service.IPurchaseOrderService;
import org.jeecg.modules.business.service.IPurchaseOrderSkuService;
import org.jeecg.modules.business.service.ISkuPromotionHistoryService;
import org.jeecg.modules.business.vo.PurchaseOrderPage;
import org.jeecg.modules.business.vo.clientPurchaseOrder.PurchaseDemand;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "商品采购订单")
@RestController
@RequestMapping("/business/purchaseOrder/client")
@Slf4j
public class ClientPurchaseController {
    private final IPurchaseOrderService purchaseOrderService;
    private final IPurchaseOrderSkuService purchaseOrderSkuService;
    private final ISkuPromotionHistoryService skuPromotionHistoryService;

    @Autowired
    public ClientPurchaseController(IPurchaseOrderService purchaseService,
                                    IPurchaseOrderSkuService purchaseOrderSkuService,
                                    ISkuPromotionHistoryService skuPromotionHistoryService) {
        this.purchaseOrderService = purchaseService;
        this.purchaseOrderSkuService = purchaseOrderSkuService;
        this.skuPromotionHistoryService = skuPromotionHistoryService;
    }

    /**
     * 分页列表查询
     *
     * @param pageNo   page number
     * @param pageSize page size
     * @return page in result
     */
    @AutoLog(value = "商品采购订单-分页列表查询")
    @ApiOperation(value = "商品采购订单-分页列表查询", notes = "商品采购订单-分页列表查询")
    @GetMapping(value = "/list")
    public Result<Page<PurchaseOrder>> queryPageList(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<PurchaseOrder> page = new Page<>(pageNo, pageSize);
        purchaseOrderService.setPageForCurrentClient(page);
        return Result.OK(page);
    }

    /**
     * 添加
     *
     * @param platformOrderIDList a list of platform order ID
     * @return the generated purchase ID
     */
    @AutoLog(value = "商品采购订单-添加")
    @ApiOperation(value = "商品采购订单-添加", notes = "商品采购订单-添加")
    @PostMapping(value = "/add")
    public Result<String> addPurchaseOrder(@RequestBody List<String> platformOrderIDList) {
        String id = purchaseOrderService.addPurchase(platformOrderIDList);
        return Result.OK(id);
    }

}
