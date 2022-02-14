package org.jeecg.modules.business.controller.admin.shippingInvoice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderStatus;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.Shop;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.IShopService;
import org.jeecg.modules.business.service.PlatformOrderShippingInvoiceService;
import org.jeecg.modules.business.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for request related to shipping invoice
 */
@Api(tags = "shippingInvoice")
@RestController
@RequestMapping("/shippingInvoice")
@Slf4j
public class InvoiceController {
    @Autowired
    private IShopService shopService;
    @Autowired
    private PlatformOrderShippingInvoiceService shippingInvoiceService;
    @Autowired
    private PlatformOrderMapper platformOrderMapper;


    @GetMapping(value = "/shopsByClient")
    public Result<List<Shop>> getShopsByClient(@RequestParam("clientID") String clientID) {
        log.info("Request for shop by client {}", clientID);
        return Result.OK(shopService.listByClient(clientID));
    }

    @PostMapping(value = "/period")
    public Result<?> getValidPeriod(@RequestBody List<String> shopIDs) {
        log.info("Request for valid period for shops: " + shopIDs.toString());
        Period period = shippingInvoiceService.getValidPeriod(shopIDs);
        if (period.isValid())
            return Result.OK(period);
        else return Result.error("No package in the selected period");
    }

    @GetMapping(value = "/preShipping/orders")
    public Result<?> getOrdersByClientAndShops(PlatformOrder platformOrder,
                                               @RequestParam("clientId") String clientId,
                                               @RequestParam(name = "shopIds", required = false) List<String> shopIDs,
                                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                               HttpServletRequest req) {
        QueryWrapper<PlatformOrder> queryWrapper = QueryGenerator.initQueryWrapper(platformOrder, req.getParameterMap());
        LambdaQueryWrapper<PlatformOrder> lambdaQueryWrapper = queryWrapper.lambda();
        lambdaQueryWrapper.in(PlatformOrder::getErpStatus, Arrays.asList(OrderStatus.Pending.getCode(), OrderStatus.Preparing.getCode()));
        lambdaQueryWrapper.isNull(PlatformOrder::getShippingInvoiceNumber);
        Page<PlatformOrder> page = new Page<>(pageNo, pageSize);
        IPage<PlatformOrder> pageList;
        log.info("Request for pre-shipping orders from client : " + clientId);
        if (shopIDs == null || shopIDs.isEmpty()) {
            lambdaQueryWrapper.inSql(PlatformOrder::getId, "SELECT po.id FROM platform_order po\n" +
                    " JOIN shop s ON po.shop_id = s.id\n" +
                    " JOIN client c ON s.owner_id = c.id WHERE c.id = '" + clientId + "'");
            pageList = platformOrderMapper.selectPage(page, lambdaQueryWrapper);
        } else {
            log.info("Specified shop IDs : " + shopIDs);
            lambdaQueryWrapper.in(PlatformOrder::getShopId, shopIDs);
            lambdaQueryWrapper.inSql(PlatformOrder::getId, "SELECT id FROM platform_order po");
            pageList = platformOrderMapper.selectPage(page, lambdaQueryWrapper);
            return Result.OK(pageList);
        }
        if (pageList.getSize() > 0) {
            return Result.OK(pageList);
        } else {
            return Result.error("No orders for selected client/shops");
        }
    }

    /**
     * Make pre-shipping invoice for specified orders
     *
     * @param param Parameters for creating a pre-shipping invoice
     * @return Result of the generation, in case of error, message will be contained,
     * in case of success, data will contain filename.
     */
    @PostMapping(value = "/preShipping/make")
    public Result<?> makePreShippingInvoice(@RequestBody PreShippingInvoiceParam param) {
        try {
            InvoiceMetaData metaData = shippingInvoiceService.makeInvoice(param);
            return Result.OK(metaData);
        } catch (UserException e) {
            return Result.error(e.getMessage());
        } catch (IOException | ParseException e) {
            log.error(e.getMessage());
            return Result.error("Sorry, server error, please try later");
        }
    }

    /**
     * Make invoice for orders indicated by param.
     *
     * @param param invoice making parameter
     * @return Result of the generation, in case of error, message will be contained,
     * in case of success, data will contain filename.
     */
    @PostMapping(value = "/make")
    public Result<?> makeInvoice(@RequestBody ShippingInvoiceParam param) {
        try {
            InvoiceMetaData metaData = shippingInvoiceService.makeInvoice(param);
            return Result.OK(metaData);
        } catch (UserException e) {
            return Result.error(e.getMessage());
        } catch (IOException | ParseException e) {
            log.error(e.getMessage());
            return Result.error("Sorry, server error, please try later");
        }
    }

    /**
     * Get binary data of the invoice file.
     *
     * @param filename name of the invoice
     * @return byte array, in case of error, an empty array will be returned.
     */
    @GetMapping(value = "/download")
    public byte[] downloadInvoice(@RequestParam("filename") String filename) {
        log.info("request for downloading shipping invoice");
        try {
            return shippingInvoiceService.getInvoiceBinary(filename);
        } catch (IOException e) {
            log.error(e.getMessage());
            return new byte[0];
        }
    }

    @GetMapping(value = "/invoiceDetail")
    public byte[] invoiceDetail(@RequestParam("invoiceNumber") String invoiceNumber) throws IOException {
        List<FactureDetail> res = shippingInvoiceService.getInvoiceDetail(invoiceNumber);
        return shippingInvoiceService.exportToExcel(res, invoiceNumber);
    }


}
