package org.jeecg.modules.business.controller.admin;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.business.entity.LogisticExpenseDetail;
import org.jeecg.modules.business.service.ILogisticExpenseDetailService;
import org.jeecg.modules.business.vo.LogisticExpenseProportion;
import org.jeecg.modules.business.vo.dashboard.PeriodLogisticProfit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.*;

/**
 * @Description: 物流开销明细
 * @Author: jeecg-boot
 * @Date: 2021-07-22
 * @Version: V1.0
 */
@Api(tags = "物流开销明细")
@RestController
@RequestMapping("/business/logisticExpenseDetail")
@Slf4j
public class LogisticExpenseDetailController extends JeecgController<LogisticExpenseDetail, ILogisticExpenseDetailService> {
    @Autowired
    private ILogisticExpenseDetailService logisticExpenseDetailService;
    private static final int START_ROWS_4PX = 6;
    private static final int HEADER_ROWS_4PX = 5;
    private static final int SHEET_NUMBER_4PX = 1;
    private static final int END_OF_DATA_4PX_EMPTY_CASE_NUMBER = 4;
    private static final int END_OF_DATA_YUN_EXPRESS_EMPTY_CASE_NUMBER = 26;

    private static final int SHEET_NUMBER_YUN_EXPRESS = 2;

    /**
     * 分页列表查询
     *
     * @param logisticExpenseDetail
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "物流开销明细-分页列表查询")
    @ApiOperation(value = "物流开销明细-分页列表查询", notes = "物流开销明细-分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(LogisticExpenseDetail logisticExpenseDetail,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<LogisticExpenseDetail> queryWrapper = QueryGenerator.initQueryWrapper(logisticExpenseDetail, req.getParameterMap());
        Page<LogisticExpenseDetail> page = new Page<LogisticExpenseDetail>(pageNo, pageSize);
        IPage<LogisticExpenseDetail> pageList = logisticExpenseDetailService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param logisticExpenseDetail
     * @return
     */
    @AutoLog(value = "物流开销明细-添加")
    @ApiOperation(value = "物流开销明细-添加", notes = "物流开销明细-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody LogisticExpenseDetail logisticExpenseDetail) {
        logisticExpenseDetailService.save(logisticExpenseDetail);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param logisticExpenseDetail
     * @return
     */
    @AutoLog(value = "物流开销明细-编辑")
    @ApiOperation(value = "物流开销明细-编辑", notes = "物流开销明细-编辑")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody LogisticExpenseDetail logisticExpenseDetail) {
        logisticExpenseDetailService.updateById(logisticExpenseDetail);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "物流开销明细-通过id删除")
    @ApiOperation(value = "物流开销明细-通过id删除", notes = "物流开销明细-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        logisticExpenseDetailService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "物流开销明细-批量删除")
    @ApiOperation(value = "物流开销明细-批量删除", notes = "物流开销明细-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.logisticExpenseDetailService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "物流开销明细-通过id查询")
    @ApiOperation(value = "物流开销明细-通过id查询", notes = "物流开销明细-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        LogisticExpenseDetail logisticExpenseDetail = logisticExpenseDetailService.getById(id);
        if (logisticExpenseDetail == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(logisticExpenseDetail);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param logisticExpenseDetail
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, LogisticExpenseDetail logisticExpenseDetail) {
        return super.exportXls(request, logisticExpenseDetail, LogisticExpenseDetail.class, "物流开销明细");
    }


    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, LogisticExpenseDetail.class);
    }

    /**
     * @param country full country name, in case of absence, all country will be taken into account
     * @param channel chinese channel name, in case of absence, all channel will be taken into account
     * @return
     */
    @GetMapping(value = "/monthlyLogisticProfit")
    public Result<PeriodLogisticProfit> monthlyLogisticProfit(
            @RequestParam("startDate") @DateTimeFormat(pattern="YYYY-MM-dd") Date startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern="YYYY-MM-dd") Date endDate,
            @RequestParam(value = "country[]", required = false) List<String> country,
            @RequestParam(value = "channel[]", required = false) List<String> channel
    ) {
        PeriodLogisticProfit monthlyProfit =
                logisticExpenseDetailService.calculateLogisticProfitOf(
                        startDate,
                        endDate,
                        country,
                        channel
                );
        return Result.OK(monthlyProfit);
    }

    @GetMapping(value = "/expenseProportionByChannel")
    public Result<List<LogisticExpenseProportion>> expenseProportionByChannel(
            @RequestParam("startDate") @DateTimeFormat(pattern="YYYY-MM-dd") Date startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern="YYYY-MM-dd") Date endDate,
            @RequestParam(value = "country[]", required = false) List<String> country,
            @RequestParam(value = "channel[]", required = false) List<String> channel
    ) {

        List<LogisticExpenseProportion> res =
                logisticExpenseDetailService.calculateLogisticExpenseProportionByChannel(startDate, endDate, country, channel);

        return Result.OK(res);
    }

    @GetMapping(value = "/expenseProportionByCountry")
    public Result<List<LogisticExpenseProportion>> expenseProportionByCountry(
            @RequestParam("startDate") @DateTimeFormat(pattern="YYYY-MM-dd") Date startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern="YYYY-MM-dd") Date endDate,
            @RequestParam(value = "country[]", required = false) List<String> country,
            @RequestParam(value = "channel[]", required = false) List<String> channel
    ) {
        List<LogisticExpenseProportion> res =
                logisticExpenseDetailService.calculateLogisticExpenseProportionByCountry(startDate, endDate, country, channel);
        return Result.OK(res);
    }

    @GetMapping(value = "/allCountry")
    public Result<List<String>> allCountry() {
        return Result.OK(logisticExpenseDetailService.allCountries());
    }

    @GetMapping(value = "/allChannel")
    public Result<List<String>> allChannel() {
        return Result.OK(logisticExpenseDetailService.allChannels());
    }

    /**
     * 通过excel导入递四方数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/import4px", method = RequestMethod.POST)
    public Result<?> import4px(HttpServletRequest request, HttpServletResponse response) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile file = entity.getValue();// 获取上传文件对象

            List<Map<String, Object>> allData;
            try (
                    InputStream in = file.getInputStream();
                    ExcelReader reader = ExcelUtil.getReader(in, SHEET_NUMBER_4PX)
            ) {
                allData = reader.read(HEADER_ROWS_4PX, START_ROWS_4PX, 2147483647);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return Result.error("文件导入失败:" + e.getMessage());
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            List<LogisticExpenseDetail> detailList = new ArrayList<>();
            for (Map<String, Object> lineData : allData) {
                LogisticExpenseDetail detail = new LogisticExpenseDetail();
                detail.setId(IdWorker.getIdStr(detail));
                detail.setCreateBy(sysUser.getUsername());
                detail.setCreateTime(new Date());
                if (lineData.values().stream().filter(Objects::isNull).count() == END_OF_DATA_4PX_EMPTY_CASE_NUMBER) {
                    // end of data
                    break;
                }
                detail.setPlatformOrderSerialId(exceptionWrapper("客户单号", lineData));
                detail.setLogisticInternalNumber(exceptionWrapper("4px单号", lineData));
                detail.setTrackingNumber(exceptionWrapper("服务商单号", lineData));
                detail.setRealWeight(new BigDecimal(exceptionWrapper("实重", lineData)));
                detail.setVolumetricWeight(new BigDecimal(exceptionWrapper("体积重", lineData)));
                detail.setChargingWeight(new BigDecimal(exceptionWrapper("计费重", lineData)));
                detail.setShippingFee(new BigDecimal(exceptionWrapper("运费", lineData)));
                detail.setDiscount(new BigDecimal(exceptionWrapper("运费优惠金额", lineData)));
                detail.setFuelSurcharge(new BigDecimal(exceptionWrapper("燃油附加费", lineData)));
                detail.setSecondDeliveryFee(new BigDecimal(exceptionWrapper("重派费", lineData)));
                detail.setRegistrationFee(new BigDecimal(exceptionWrapper("挂号费", lineData)));
                detail.setVat(new BigDecimal(exceptionWrapper("代收VAT税", lineData)));
                detail.setTotalFee(new BigDecimal(exceptionWrapper("合计(RMB)", lineData)));
                detail.setLogisticCompanyId("1419601960440684546");
                detailList.add(detail);
            }
            logisticExpenseDetailService.saveBatch(detailList);
            return Result.OK("文件导入成功！数据行数:" + detailList.size());
        }
        return Result.OK("文件导入失败！");
    }

    private String exceptionWrapper(String col, Map<String, Object> table) {
        Object colValue = table.get(col);
        if (colValue == null) {
            throw new RuntimeException("Column " + col + " does not exist");
        }
        return colValue.toString();
    }

    /**
     * 通过excel导入云途数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importYunExpress", method = RequestMethod.POST)
    public Result<?> importYunExpress(HttpServletRequest request, HttpServletResponse response) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile file = entity.getValue();// 获取上传文件对象

            List<Map<String, Object>> allData;
            try (
                    InputStream in = file.getInputStream();
                    ExcelReader reader = ExcelUtil.getReader(in, SHEET_NUMBER_YUN_EXPRESS)
            ) {
                allData = reader.readAll();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return Result.error("文件导入失败:" + e.getMessage());
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            List<LogisticExpenseDetail> detailList = new ArrayList<>();
            for (Map<String, Object> lineData : allData) {
                LogisticExpenseDetail detail = new LogisticExpenseDetail();
                detail.setId(IdWorker.getIdStr(detail));
                detail.setCreateBy(sysUser.getUsername());
                detail.setCreateTime(new Date());
                if (lineData.values().stream().filter(Objects::isNull).count() == END_OF_DATA_YUN_EXPRESS_EMPTY_CASE_NUMBER) {
                    // end of data
                    break;
                }
                detail.setPlatformOrderSerialId(exceptionWrapper("客户单号", lineData));
                detail.setLogisticInternalNumber(exceptionWrapper("运单号", lineData));
                detail.setTrackingNumber(exceptionWrapper("服务商单号", lineData));
                detail.setRealWeight(new BigDecimal(exceptionWrapper("实重", lineData)));
                detail.setVolumetricWeight(new BigDecimal(exceptionWrapper("材积重", lineData)));
                detail.setChargingWeight(new BigDecimal(exceptionWrapper("计费重量", lineData)));
                detail.setShippingFee(new BigDecimal(exceptionWrapper("运费", lineData)));
                detail.setFuelSurcharge(new BigDecimal(exceptionWrapper("燃油费", lineData)));
                detail.setAdditionalFee(new BigDecimal(exceptionWrapper("偏远附加费", lineData)));
                detail.setSecondDeliveryFee(new BigDecimal(exceptionWrapper("重派费", lineData)));
                detail.setRegistrationFee(new BigDecimal(exceptionWrapper("挂号费", lineData)));
                detail.setVat(new BigDecimal(exceptionWrapper("VAT增值税", lineData)));
                detail.setVatServiceFee(new BigDecimal(exceptionWrapper("其他费用", lineData)));
                detail.setTotalFee(new BigDecimal(exceptionWrapper("总运费", lineData)));
                detail.setLogisticCompanyId("1419602297851469825");
                detailList.add(detail);
            }
            logisticExpenseDetailService.saveBatch(detailList);
            return Result.OK("文件导入成功！数据行数:" + detailList.size());
        }
        return Result.OK("文件导入失败！");
    }

    /**
     * 通过excel导入淼信数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importMs", method = RequestMethod.POST)
    public Result<?> importMs(HttpServletRequest request, HttpServletResponse response) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile file = entity.getValue();// 获取上传文件对象

            List<Map<String, Object>> allData;
            try (
                    InputStream in = file.getInputStream();
                    ExcelReader reader = ExcelUtil.getReader(in)
            ) {
                allData = reader.readAll();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return Result.error("文件导入失败:" + e.getMessage());
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            List<LogisticExpenseDetail> detailList = new ArrayList<>();
            for (Map<String, Object> lineData : allData) {
                LogisticExpenseDetail detail = new LogisticExpenseDetail();
                detail.setId(IdWorker.getIdStr(detail));
                detail.setCreateBy(sysUser.getUsername());
                detail.setCreateTime(new Date());
                detail.setVirtualTrackingNumber(exceptionWrapper("原单号", lineData));
                detail.setLogisticInternalNumber(exceptionWrapper("内部单号", lineData));
                detail.setTrackingNumber(exceptionWrapper("转单号", lineData));
                detail.setChargingWeight(new BigDecimal(exceptionWrapper("计费重", lineData)));
                detail.setShippingFee(new BigDecimal(exceptionWrapper("运费", lineData)));
                detail.setRegistrationFee(new BigDecimal(exceptionWrapper("杂费", lineData)));
                detail.setTotalFee(new BigDecimal(exceptionWrapper("总金额", lineData)));
                detail.setLogisticCompanyId("1419602275764264961");
                detailList.add(detail);
            }
            logisticExpenseDetailService.saveBatch(detailList);
            return Result.OK("文件导入成功！数据行数:" + detailList.size());
        }
        return Result.OK("文件导入失败！");
    }

}
