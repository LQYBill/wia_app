package org.jeecg.modules.business.controller.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.controller.client.requestParams.PurchaseRequest;
import org.jeecg.modules.business.entity.PurchaseOrder;
import org.jeecg.modules.business.service.IPurchaseOrderService;
import org.jeecg.modules.business.service.IPurchaseOrderSkuService;
import org.jeecg.modules.business.service.ISkuPromotionHistoryService;
import org.jeecg.modules.business.service.impl.purchase.PurchaseShippingQuoteService;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.jeecg.modules.business.vo.clientPurchaseOrder.PurchaseShippingQuote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Api(tags = "商品采购订单")
@RestController
@RequestMapping("/business/purchaseOrder/client")
@Slf4j
public class ClientPurchaseController {
    @Autowired
    private PurchaseShippingQuoteService shippingQuotes;

    @PostMapping("/quote")
    public Result<PurchaseShippingQuote> quote(@RequestBody List<SkuQuantity> quantities) throws UserException {
        return Result.OK(shippingQuotes.preview(quantities));
    }

    @GetMapping("/shippingQuote")
    public Result<PurchaseShippingQuote> shippingQuote(@RequestParam String purchaseId) throws UserException {
        PurchaseOrder order = purchaseOrderService.getById(purchaseId);
        if (order == null || !shippingQuotes.currentClient().getId().equals(order.getClientId())) {
            throw new UserException("Purchase not found");
        }
        return Result.OK(shippingQuotes.saved(purchaseId));
    }

    @ExceptionHandler(UserException.class)
    public Result<?> purchaseError(UserException error) {
        return Result.error(400, error.getMessage());
    }
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
     * Add new purchase order.
     *
     * @param purchaseRequest request that contains necessary information
     * @return the generated purchase ID
     */
    @AutoLog(value = "商品采购订单-添加")
    @ApiOperation(value = "商品采购订单-添加", notes = "商品采购订单-添加")
    @PostMapping(value = "/add")
    public Result<String> addPurchaseOrder(@RequestBody PurchaseRequest purchaseRequest) throws UserException {
        if (purchaseRequest.getPlatformOrderIDList() == null || purchaseRequest.getPlatformOrderIDList().isEmpty()) {
            return Result.OK(purchaseOrderService.addQuotedSkuPurchase(purchaseRequest.getSkuQuantity(),
                    purchaseRequest.getInvoiceEntityId(), purchaseRequest.getQuoteId()));
        }
        String id = purchaseOrderService.addPurchase(
                purchaseRequest.getSkuQuantity(),
                purchaseRequest.getPlatformOrderIDList(),
                purchaseRequest.getInvoiceEntityId()
        );
        log.info("Order list: {}", purchaseRequest.getPlatformOrderIDList());
        return Result.OK(id);
    }

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
