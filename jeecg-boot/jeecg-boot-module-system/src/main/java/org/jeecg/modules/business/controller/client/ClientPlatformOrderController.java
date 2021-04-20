package org.jeecg.modules.business.controller.client;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.vo.clientPlatformOrder.ClientPlatformOrderPage;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;
import org.jeecg.modules.business.vo.clientPlatformOrder.PurchaseConfirmation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description: platform order table for a certain client
 * @Author: Wenke
 * @Date: 2021-04-17
 * @Version: V1.000001
 */
@Api(tags = "平台订单表")
@RestController
@RequestMapping("/business/clientPlatformOrder")
@Slf4j
public class ClientPlatformOrderController {
    private final IPlatformOrderService platformOrderService;

    @Autowired
    public ClientPlatformOrderController(IPlatformOrderService platformOrderService) {
        this.platformOrderService = platformOrderService;
    }

    /**
     * Query all platform orders for current client.
     *
     * @return all platform orders of current client in a Result object
     */
    @AutoLog(value = "当前客户的平台订单列表查询")
    @ApiOperation(value = "当前客户的平台订单列表查询", notes = "当前客户的平台订单列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<ClientPlatformOrderPage>> queryAllPlatformOrder(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("Query for client platform orders");
        IPage<ClientPlatformOrderPage> page = new Page<>(pageNo, pageSize);
        platformOrderService.initPlatformOrderPage(page);
        return Result.OK(page);
    }


    @PostMapping(value = "/computeInfo", consumes = "application/json", produces = "application/json")
    public Result<OrdersStatisticData> queryOrdersStatisticInfo(@RequestBody List<String> orderIds) {
        log.info("Calculating statistic information for orders: {}", orderIds);
        OrdersStatisticData ordersData = platformOrderService.getPlatformOrdersStatisticData(orderIds);
        log.info("Got statistic information: {}", ordersData);
        return Result.OK(ordersData);
    }


    /**
     * Query a platform order by its identifier.
     *
     * @param id the identifier of the platform order
     * @return {@code Result} of platform order, or {@code Result} with error message in case of failure.
     */
    @AutoLog(value = "平台订单表-通过id查询")
    @ApiOperation(value = "平台订单表-通过id查询", notes = "平台订单表-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        PlatformOrder platformOrder = platformOrderService.getById(id);
        if (platformOrder == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(platformOrder);
    }

    /**
     * Query a certain platform order's content by its identifier.
     *
     * @param id the identifier of the platform order
     * @return its content
     */
    @AutoLog(value = "平台订单内容通过主表ID查询")
    @ApiOperation(value = "平台订单内容主表ID查询", notes = "平台订单内容-通主表ID查询")
    @GetMapping(value = "/queryPlatformOrderContentByMainId")
    public Result<?> queryPlatformOrderContentListByMainId(@RequestParam(name = "id") String id) {
        List<PlatformOrderContent> platformOrderContentList = platformOrderService.selectByMainId(id);
        IPage<PlatformOrderContent> page = new Page<>();
        page.setRecords(platformOrderContentList);
        page.setTotal(platformOrderContentList.size());
        return Result.OK(page);
    }


    @PostMapping(value = "/purchase", consumes = "application/json", produces = "application/json")
    public Result<PurchaseConfirmation> purchaseOrder(@RequestBody List<String> orderIds) {
        log.info("One client purchase order");
        PurchaseConfirmation d = platformOrderService.purchaseOrder(orderIds);
        return Result.OK(d);
    }


}
