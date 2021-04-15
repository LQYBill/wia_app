package org.jeecg.modules.business.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.service.IClientPlatformOrderService;
import org.jeecg.modules.business.service.IPlatformOrderContentService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.vo.OrdersStatisticData;
import org.jeecg.modules.business.vo.PlatformOrderPage;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: 平台订单表
 * @Author: jeecg-boot
 * @Date: 2021-04-08
 * @Version: V1.0
 */
@Api(tags = "平台订单表")
@RestController
@RequestMapping("/business/clientPlatformOrder")
@Slf4j
public class ClientPlatformOrderController {
    private final IPlatformOrderService platformOrderService;
    private final IPlatformOrderContentService platformOrderContentService;
    private final IClientPlatformOrderService clientPlatformOrderService;

    @Autowired
    public ClientPlatformOrderController(IPlatformOrderService platformOrderService,
                                         IPlatformOrderContentService platformOrderContentService,
                                         IClientPlatformOrderService clientPlatformOrderService) {
        this.platformOrderService = platformOrderService;
        this.platformOrderContentService = platformOrderContentService;
        this.clientPlatformOrderService = clientPlatformOrderService;
    }

    /**
     * Query all platform orders for current client.
     *
     * @return all platform orders of current client
     */
    @AutoLog(value = "当前客户的平台订单列表查询")
    @ApiOperation(value = "当前客户的平台订单列表查询", notes = "当前客户的平台订单列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<PlatformOrderPage>> queryAllPlatformOrder() {
        log.info("Query for client platform orders");
        IPage<PlatformOrderPage> page = new Page<>();
        List<PlatformOrderPage> platformOrderPages = clientPlatformOrderService.getPlatformOrderList();
        page.setRecords(platformOrderPages);
        page.setTotal(platformOrderPages.size());
        return Result.OK(page);
    }


    @PostMapping(value = "/computeInfo", consumes = "application/json", produces = "application/json")
    public Result<OrdersStatisticData> queryOrdersStatisticInfo(@RequestBody List<String> orderIds) {
        log.info("Calculating statistic information for orders: {}", orderIds);
        OrdersStatisticData ordersData = clientPlatformOrderService.getPlatformOrdersStatisticData(orderIds);
        return Result.OK(ordersData);
    }


    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "平台订单表-通过id查询")
    @ApiOperation(value = "平台订单表-通过id查询", notes = "平台订单表-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        PlatformOrder platformOrder = platformOrderService.getById(id);
        if (platformOrder == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(platformOrder);
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "平台订单内容通过主表ID查询")
    @ApiOperation(value = "平台订单内容主表ID查询", notes = "平台订单内容-通主表ID查询")
    @GetMapping(value = "/queryPlatformOrderContentByMainId")
    public Result<?> queryPlatformOrderContentListByMainId(@RequestParam(name = "id", required = true) String id) {
        List<PlatformOrderContent> platformOrderContentList = platformOrderContentService.selectByMainId(id);
        return Result.OK(platformOrderContentList);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param platformOrder
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, PlatformOrder platformOrder) {
        // Step.1 组装查询条件查询数据
        QueryWrapper<PlatformOrder> queryWrapper = QueryGenerator.initQueryWrapper(platformOrder, request.getParameterMap());
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

        //Step.2 获取导出数据
        List<PlatformOrder> queryList = platformOrderService.list(queryWrapper);
        // 过滤选中数据
        String selections = request.getParameter("selections");
        List<PlatformOrder> platformOrderList;
        if (oConvertUtils.isEmpty(selections)) {
            platformOrderList = queryList;
        } else {
            List<String> selectionList = Arrays.asList(selections.split(","));
            platformOrderList = queryList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
        }

        // Step.3 组装pageList
        List<PlatformOrderPage> pageList = new ArrayList<PlatformOrderPage>();
        for (PlatformOrder main : platformOrderList) {
            PlatformOrderPage vo = new PlatformOrderPage();
            BeanUtils.copyProperties(main, vo);
            List<PlatformOrderContent> platformOrderContentList = platformOrderContentService.selectByMainId(main.getId());
            vo.setPlatformOrderContentList(platformOrderContentList);
            pageList.add(vo);
        }

        // Step.4 AutoPoi 导出Excel
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject(NormalExcelConstants.FILE_NAME, "平台订单表列表");
        mv.addObject(NormalExcelConstants.CLASS, PlatformOrderPage.class);
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("平台订单表数据", "导出人:" + sysUser.getRealname(), "平台订单表"));
        mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        return mv;
    }

}
