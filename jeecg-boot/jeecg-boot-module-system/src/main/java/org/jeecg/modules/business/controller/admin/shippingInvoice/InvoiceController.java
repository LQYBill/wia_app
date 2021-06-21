package org.jeecg.modules.business.controller.admin.shippingInvoice;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.business.entity.Shop;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.IShopService;
import org.jeecg.modules.business.vo.Period;
import org.jeecg.modules.business.vo.ShippingInvoiceParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
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
    private IClientService clientService;
    @Autowired
    private IShopService shopService;

    @Value("${jeecg.path.shippingInvoiceDir}")
    private String DIR;

    @GetMapping(value = "/shopsByClient")
    public Result<List<Shop>> getShopsByClient(@RequestParam("clientID") String clientID) {
        log.info("Request for shop by client {}", clientID);
        return Result.OK(shopService.listByClient(clientID));
    }

    @PostMapping(value = "/period")
    public Result<Period> getValidPeriod(@RequestBody List<String> shopIDs) {
        log.info(shopIDs.toString());
        return Result.OK(new Period(Instant.now(), Instant.now()));
    }

    @PostMapping(value = "/make")
    public Result<String> makeInvoice(@RequestBody ShippingInvoiceParam param) {
        log.info(param.toString());
        return Result.OK("Hello World.xlsx");
    }

    @GetMapping(value = "/download")
    public byte[] downloadInvoice(@RequestParam("filename") String filename) throws IOException {
        log.info("request for downloading shipping invoice");
        Path template = Paths.get(DIR, "template.xlsx");
        return Files.readAllBytes(template);
    }


}
