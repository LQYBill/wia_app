package org.jeecg.modules.business.controller.admin.shippingInvoice;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.entity.Shop;
import org.jeecg.modules.business.service.IShopService;
import org.jeecg.modules.business.service.PlatformOrderShippingInvoiceService;
import org.jeecg.modules.business.vo.FactureDetail;
import org.jeecg.modules.business.vo.Period;
import org.jeecg.modules.business.vo.ShippingInvoiceParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
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


    @GetMapping(value = "/shopsByClient")
    public Result<List<Shop>> getShopsByClient(@RequestParam("clientID") String clientID) {
        log.info("Request for shop by client {}", clientID);
        return Result.OK(shopService.listByClient(clientID));
    }

    @PostMapping(value = "/period")
    public Result<?> getValidPeriod(@RequestBody List<String> shopIDs) {
        log.info("Request for valide period for shops: " + shopIDs.toString());
        Period period = shippingInvoiceService.getValidePeriod(shopIDs);
        if (period.isValid())
            return Result.OK(period);
        else return Result.error("No package in the selected period");
    }

    /**
     * Make invoice of orders indicated by param.
     *
     * @param param invoice making parameter
     * @return Result of the generation, in case of error, message will be contained,
     * in case of success, data will contain filename.
     */
    @PostMapping(value = "/make")
    public Result<?> makeInvoice(@RequestBody ShippingInvoiceParam param) {
        try {
            String filename = shippingInvoiceService.makeInvoice(param);
            return Result.OK(filename);
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
        return shippingInvoiceService.ExportToExcel(res);
    }


}
