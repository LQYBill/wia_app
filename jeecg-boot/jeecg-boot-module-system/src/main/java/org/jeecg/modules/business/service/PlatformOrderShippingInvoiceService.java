package org.jeecg.modules.business.service;

import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.shippingInvoice.ShippingInvoice;
import org.jeecg.modules.business.domain.shippingInvoice.ShippingInvoiceFactory;
import org.jeecg.modules.business.entity.ShippingInvoiceEntity;
import org.jeecg.modules.business.mapper.*;
import org.jeecg.modules.business.vo.Period;
import org.jeecg.modules.business.vo.ShippingInvoiceParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Date;
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
    @Autowired
    ISkuDeclaredValueService skuDeclaredValueService;
    @Autowired
    IPlatformOrderService platformOrderService;
    @Autowired
    CountryService countryService;
    @Autowired
    ExchangeRatesMapper exchangeRatesMapper;

    @Value("${jeecg.path.shippingTemplatePath_EU}")
    private String INVOICE_TEMPLATE_EU;

    @Value("${jeecg.path.shippingTemplatePath_US}")
    private String INVOICE_TEMPLATE_US;

    @Value("${jeecg.path.shippingInvoiceDir}")
    private String DIR;

    public Period getValidePeriod(List<String> shopIDs) {
        Date begin = platformOrderMapper.findEarliestUninvoicedPlatformOrder(shopIDs);
        Date end = platformOrderMapper.findLatestUninvoicedPlatformOrder(shopIDs);
        return new Period(begin, end);
    }

    /**
     * Make a invoice based on parameters.
     *
     * @param param the parameters to make the invoice
     * @return identifiant name of the invoice, can be used to in {@code getInvoiceBinary}.
     * @throws UserException  exception due to error of user input, message will contain detail
     * @throws ParseException exception because of format of "start" and "end" date does not follow
     *                        pattern: "yyyy-MM-dd"
     * @throws IOException    exception related to invoice file IO.
     */
    @Transactional
    public String makeInvoice(ShippingInvoiceParam param) throws UserException, ParseException, IOException {
        // Creates factory
        ShippingInvoiceFactory factory = new ShippingInvoiceFactory(
                platformOrderService,
                clientMapper,
                logisticChannelPriceMapper,
                platformOrderContentService,
                skuDeclaredValueService,
                countryService,
                exchangeRatesMapper);
        // Creates invoice by factory
        ShippingInvoice invoice = factory.createInvoice(param.clientID(),
                param.shopIDs(),
                param.start(),
                param.end()
        );
        // Chooses invoice template based on client's preference on currency
        Path src;
        if (invoice.client().getCurrency().equals("USD")) {
            src = Paths.get(INVOICE_TEMPLATE_US);
        } else {
            src = Paths.get(INVOICE_TEMPLATE_EU);
        }
        // Writes invoice content to a new excel file
         String filename = "Invoice N°" + invoice.code() + " (" + invoice.client().getInvoiceEntity() + ").xlsx";
        Path out = Paths.get(DIR, filename);
        if (!Files.exists(out, LinkOption.NOFOLLOW_LINKS)) {
            Files.copy(src, out);
            invoice.toExcelFile(out);
        }
        // save to DB
        ShippingInvoiceEntity shippingInvoiceEntity = ShippingInvoiceEntity.of(
                invoice.code(),
                invoice.totalAmount(),
                invoice.reducedAmount(),
                invoice.paidAmount()
        );
        shippingInvoiceMapper.save(shippingInvoiceEntity);

        return filename;
    }

    /**
     * Returns byte stream of a invoice file
     *
     * @param filename identifiant name of the invoice file
     * @return byte array of the file
     * @throws IOException error when reading file
     */
    public byte[] getInvoiceBinary(String filename) throws IOException {
        Path out = Paths.get(DIR, filename);
        return Files.readAllBytes(out);
    }
}
