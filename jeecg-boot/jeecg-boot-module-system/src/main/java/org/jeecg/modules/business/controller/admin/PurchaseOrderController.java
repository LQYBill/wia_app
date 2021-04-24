package org.jeecg.modules.business.controller.admin;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeecg.modules.business.vo.PromotionDetail;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.business.entity.PurchaseOrderSku;
import org.jeecg.modules.business.entity.PurchaseOrder;
import org.jeecg.modules.business.vo.PurchaseOrderPage;
import org.jeecg.modules.business.service.IPurchaseOrderService;
import org.jeecg.modules.business.service.IPurchaseOrderSkuService;
import org.jeecg.modules.business.service.ISkuPromotionHistoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.aspect.annotation.AutoLog;

/**
 * @Description: API Handler related admin purchase order
 * @Author: Wenke
 * @Date: 2021-04-24
 * @Version: V1.0
 */
@Api(tags = "Administrator side purchase order API")
@RestController
@RequestMapping("/business/purchaseOrder")
@Slf4j
public class PurchaseOrderController {
    private final IPurchaseOrderService purchaseOrderService;
    private final IPurchaseOrderSkuService purchaseOrderSkuService;
    private final ISkuPromotionHistoryService skuPromotionHistoryService;

    @Autowired
    public PurchaseOrderController(IPurchaseOrderService purchaseOrderService,
                                   IPurchaseOrderSkuService purchaseOrderSkuService,
                                   ISkuPromotionHistoryService skuPromotionHistoryService) {
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderSkuService = purchaseOrderSkuService;
        this.skuPromotionHistoryService = skuPromotionHistoryService;
    }

    /**
     * Page query for purchase order
     *
     * @param purchaseOrder
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "商品采购订单-分页列表查询")
    @ApiOperation(value = "商品采购订单-分页列表查询", notes = "商品采购订单-分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(PurchaseOrder purchaseOrder,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<PurchaseOrder> queryWrapper = QueryGenerator.initQueryWrapper(purchaseOrder, req.getParameterMap());
        Page<PurchaseOrder> page = new Page<>(pageNo, pageSize);
        IPage<PurchaseOrder> pageList = purchaseOrderService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param purchaseOrderPage
     * @return
     */
    @AutoLog(value = "商品采购订单-添加")
    @ApiOperation(value = "商品采购订单-添加", notes = "商品采购订单-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody PurchaseOrderPage purchaseOrderPage) {
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        BeanUtils.copyProperties(purchaseOrderPage, purchaseOrder);
        purchaseOrderService.saveMain(purchaseOrder, purchaseOrderPage.getPurchaseOrderSkuList(), purchaseOrderPage.getSkuPromotionHistoryList());
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param purchaseOrderPage
     * @return
     */
    @AutoLog(value = "商品采购订单-编辑")
    @ApiOperation(value = "商品采购订单-编辑", notes = "商品采购订单-编辑")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody PurchaseOrderPage purchaseOrderPage) {
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        BeanUtils.copyProperties(purchaseOrderPage, purchaseOrder);
        PurchaseOrder purchaseOrderEntity = purchaseOrderService.getById(purchaseOrder.getId());
        if (purchaseOrderEntity == null) {
            return Result.error("未找到对应数据");
        }
        purchaseOrderService.updateMain(purchaseOrder, purchaseOrderPage.getPurchaseOrderSkuList(), purchaseOrderPage.getSkuPromotionHistoryList());
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "商品采购订单-通过id删除")
    @ApiOperation(value = "商品采购订单-通过id删除", notes = "商品采购订单-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        purchaseOrderService.delMain(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "商品采购订单-批量删除")
    @ApiOperation(value = "商品采购订单-批量删除", notes = "商品采购订单-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.purchaseOrderService.delBatchMain(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "商品采购订单-通过id查询")
    @ApiOperation(value = "商品采购订单-通过id查询", notes = "商品采购订单-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        PurchaseOrder purchaseOrder = purchaseOrderService.getById(id);
        if (purchaseOrder == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(purchaseOrder);

    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "商品采购订单SKU-通过主表ID查询")
    @ApiOperation(value = "商品采购订单SKU-通过主表ID查询", notes = "商品采购订单SKU-通过主表ID查询")
    @GetMapping(value = "/queryPurchaseOrderSkuByMainId")
    public Result<?> queryPurchaseOrderSkuListByMainId(@RequestParam(name = "id", required = true) String id) {
        log.info("query content by id: {}", id);
        List<PurchaseOrderSku> purchaseOrderSkuList = purchaseOrderSkuService.selectByMainId(id);
        IPage<PurchaseOrderSku> page = new Page<>();
        page.setRecords(purchaseOrderSkuList);
        page.setTotal(purchaseOrderSkuList.size());
        return Result.OK(page);
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "SKU采购折扣历史-通过主表ID查询")
    @ApiOperation(value = "SKU采购折扣历史-通过主表ID查询", notes = "SKU采购折扣历史-通过主表ID查询")
    @GetMapping(value = "/querySkuPromotionHistoryByMainId")
    public Result<?> querySkuPromotionHistoryListByMainId(@RequestParam(name = "id", required = true) String id) {
        List<PromotionDetail> skuPromotionHistoryList = skuPromotionHistoryService.selectByMainId(id);
        IPage<PromotionDetail> page = new Page<>();
        page.setRecords(skuPromotionHistoryList);
        page.setTotal(skuPromotionHistoryList.size());
        return Result.OK(page);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param purchaseOrder
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, PurchaseOrder purchaseOrder) {
//        // Step.1 组装查询条件查询数据
//        QueryWrapper<PurchaseOrder> queryWrapper = QueryGenerator.initQueryWrapper(purchaseOrder, request.getParameterMap());
//        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
//
//        //Step.2 获取导出数据
//        List<PurchaseOrder> queryList = purchaseOrderService.list(queryWrapper);
//        // 过滤选中数据
//        String selections = request.getParameter("selections");
//        List<PurchaseOrder> purchaseOrderList = new ArrayList<PurchaseOrder>();
//        if (oConvertUtils.isEmpty(selections)) {
//            purchaseOrderList = queryList;
//        } else {
//            List<String> selectionList = Arrays.asList(selections.split(","));
//            purchaseOrderList = queryList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
//        }
//
//        // Step.3 组装pageList
//        List<PurchaseOrderPage> pageList = new ArrayList<PurchaseOrderPage>();
//        for (PurchaseOrder main : purchaseOrderList) {
//            PurchaseOrderPage vo = new PurchaseOrderPage();
//            BeanUtils.copyProperties(main, vo);
//            List<PurchaseOrderSku> purchaseOrderSkuList = purchaseOrderSkuService.selectByMainId(main.getId());
//            vo.setPurchaseOrderSkuList(purchaseOrderSkuList);
//            List<PromotionDetail> skuPromotionHistoryList = skuPromotionHistoryService.selectByMainId(main.getId());
//            vo.setSkuPromotionHistoryList(skuPromotionHistoryList);
//            pageList.add(vo);
//        }
//
//        // Step.4 AutoPoi 导出Excel
//        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
//        mv.addObject(NormalExcelConstants.FILE_NAME, "商品采购订单列表");
//        mv.addObject(NormalExcelConstants.CLASS, PurchaseOrderPage.class);
//        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("商品采购订单数据", "导出人:" + sysUser.getRealname(), "商品采购订单"));
//        mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
//        return mv;
        return null;
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
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile file = entity.getValue();// 获取上传文件对象
            ImportParams params = new ImportParams();
            params.setTitleRows(2);
            params.setHeadRows(1);
            params.setNeedSave(true);
            try {
                List<PurchaseOrderPage> list = ExcelImportUtil.importExcel(file.getInputStream(), PurchaseOrderPage.class, params);
                for (PurchaseOrderPage page : list) {
                    PurchaseOrder po = new PurchaseOrder();
                    BeanUtils.copyProperties(page, po);
                    purchaseOrderService.saveMain(po, page.getPurchaseOrderSkuList(), page.getSkuPromotionHistoryList());
                }
                return Result.OK("文件导入成功！数据行数:" + list.size());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Result.error("文件导入失败:" + e.getMessage());
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Result.OK("文件导入失败！");
    }

    /**
     * Download payment document of the purchase by document's name,
     * The file will be write back in binary format.
     *
     * @param filename document's name
     */
    @RequestMapping(value = "/downloadFile", method = RequestMethod.GET)
    public void downloadFile(@RequestParam String filename, HttpServletResponse response) throws IOException {
        byte[] out = purchaseOrderService.downloadPaymentDocumentOfPurchase(filename);
        response.setContentType("application/octet-stream;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;");
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.getOutputStream().write(out);
    }


    /**
     * Upload a payment file for a certain purchase order.
     * The parameter of the POST request should contain <b>purchase ID and file</b> entry.
     *
     * @param request the post request
     * @return error or success message
     * @throws IOException IO error when saving the file.
     */
    @AutoLog(value = "Upload payment document")
    @ApiOperation(value = "Upload payment document", notes = "Upload payment document")
    @PostMapping(value = "/uploadPaymentFile", consumes = {"multipart/form-data"})
    public Result<?> uploadPaymentFile(HttpServletRequest request) throws IOException {
        String purchaseID = request.getParameter("purchaseID");
        if (purchaseID == null) {
            return Result.error("Missing value: purchaseID");
        }
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile file = multipartRequest.getFile("file");// 获取上传文件对象
        if (file == null) {
            return Result.error("Missing file.");
        }
        purchaseOrderService.savePaymentDocumentForPurchase(purchaseID, file);
        return Result.OK("Payment file upload success");
    }


}
