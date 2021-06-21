package org.jeecg.modules.business.controller.admin.shippingInvoice;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.business.entity.Shop;
import org.jeecg.modules.business.service.IShopService;
import org.jeecg.modules.business.service.PlatformOrderShippingInvoiceService;
import org.jeecg.modules.business.vo.Period;
import org.jeecg.modules.business.vo.ShippingInvoiceParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
    public Result<Period> getValidPeriod(@RequestBody List<String> shopIDs) {
        log.info("Request for valide period for shops: " + shopIDs.toString());
        Period period = shippingInvoiceService.getValidePeriod(shopIDs);
        return Result.OK(period);
    }

    @PostMapping(value = "/make")
    public Result<String> makeInvoice(@RequestBody ShippingInvoiceParam param) {
        String filename = shippingInvoiceService.makeInvoice(param);
        return Result.OK(filename);
    }

    @GetMapping(value = "/download")
    public byte[] downloadInvoice(@RequestParam("filename") String filename) {
        log.info("request for downloading shipping invoice");
        try {
            return shippingInvoiceService.getInvoiceBinary(filename);
        } catch (IOException e) {
            return new byte[0];
        }
    }


}
