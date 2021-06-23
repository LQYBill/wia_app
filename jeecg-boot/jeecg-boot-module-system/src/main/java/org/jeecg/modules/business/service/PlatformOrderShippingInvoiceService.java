package org.jeecg.modules.business.service;

import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.shippingInvoice.ShippingInvoice;
import org.jeecg.modules.business.domain.shippingInvoice.ShippingInvoiceFactory;
import org.jeecg.modules.business.mapper.ClientMapper;
import org.jeecg.modules.business.mapper.LogisticChannelPriceMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.mapper.ShippingInvoiceMapper;
import org.jeecg.modules.business.vo.Period;
import org.jeecg.modules.business.vo.ShippingInvoiceParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;

@Service
public class PlatformOrderShippingInvoiceService {

    @Autowired
    ShippingInvoiceMapper shippingInvoiceMapper;
    @Autowired
    PlatformOrderMapper platformOrderMapper;
    @Autowired
    ClientMapper clientMapper;
    @Autowired
    LogisticChannelPriceMapper logisticChannelPriceMapper;
    @Autowired
    IPlatformOrderContentService platformOrderContentService;

    @Value("${jeecg.path.shippingTemplatePath_EU}")
    private String INVOICE_TEMPLATE_EU;

    @Value("${jeecg.path.shippingTemplatePath_US}")
    private String INVOICE_TEMPLATE_US;

    @Value("${jeecg.path.shippingInvoiceDir}")
    private String DIR;

    public Period getValidePeriod(List<String> shopIDs) {
        long beginMile = shippingInvoiceMapper.findEarliestUninvoicedPlatformOrder(shopIDs);
        long endMile = shippingInvoiceMapper.findLastestUninvoicedPlatformOrder(shopIDs);
        Instant begin = Instant.ofEpochMilli(beginMile);
        Instant end = Instant.ofEpochMilli(endMile);
        return new Period(begin, end);
    }

    public String makeInvoice(ShippingInvoiceParam param) throws UserException, ParseException, IOException {
        ShippingInvoiceFactory factory = new ShippingInvoiceFactory(
                platformOrderMapper,
                clientMapper,
                logisticChannelPriceMapper,
                platformOrderContentService
        );
        ShippingInvoice invoice = factory.createInvoice(param.clientID(),
                param.shopIDs(),
                param.start(),
                param.end()
        );

        Path src;
        if (invoice.client().getCurrency().equals("US")) {
            src = Paths.get(INVOICE_TEMPLATE_US);
        } else {
            src = Paths.get(INVOICE_TEMPLATE_EU);
        }

        String filename = invoice.code() + "xlsx";
        Path out = Paths.get(DIR, filename);
        if (!Files.exists(out, LinkOption.NOFOLLOW_LINKS)) {
            Files.copy(src, out);
            invoice.toExcelFile(out);
        }
        return filename;
    }

    public byte[] getInvoiceBinary(String filename) throws IOException {
        Path out = Paths.get(DIR, filename);
        return Files.readAllBytes(out);
    }
}
