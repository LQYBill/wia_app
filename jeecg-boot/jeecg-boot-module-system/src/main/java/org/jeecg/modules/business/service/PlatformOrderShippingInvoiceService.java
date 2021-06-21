package org.jeecg.modules.business.service;

import org.jeecg.modules.business.mapper.ShippingInvoiceMapper;
import org.jeecg.modules.business.vo.Period;
import org.jeecg.modules.business.vo.ShippingInvoiceParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@Service
public class PlatformOrderShippingInvoiceService {
    ShippingInvoiceMapper shippingInvoiceMapper;

    @Value("${jeecg.path.shippingInvoiceDir}")
    private String DIR;

    public Period getValidePeriod(List<String> shopIDs) {
        long beginMile = shippingInvoiceMapper.findEarliestUninvoicedPlatformOrder(shopIDs);
        long endMile = shippingInvoiceMapper.findLastestUninvoicedPlatformOrder(shopIDs);
        Instant begin = Instant.ofEpochMilli(beginMile);
        Instant end = Instant.ofEpochMilli(endMile);
        return new Period(begin, end);
    }

    public String makeInvoice(ShippingInvoiceParam param) {

        return "Hello World.xlsx";
    }

    public byte[] getInvoiceBinary(String filename) throws IOException {
        Path template = Paths.get(DIR, "template.xlsx");
        return Files.readAllBytes(template);
    }
}
