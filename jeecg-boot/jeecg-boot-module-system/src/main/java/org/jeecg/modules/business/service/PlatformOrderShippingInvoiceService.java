package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.excel.SheetManager;
import org.jeecg.modules.business.domain.shippingInvoice.ShippingInvoice;
import org.jeecg.modules.business.domain.shippingInvoice.ShippingInvoiceFactory;
import org.jeecg.modules.business.entity.ShippingInvoiceEntity;
import org.jeecg.modules.business.mapper.*;
import org.jeecg.modules.business.vo.*;
import org.jetbrains.annotations.NotNull;
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
    ShopMapper shopMapper;
    @Autowired
    LogisticChannelPriceMapper logisticChannelPriceMapper;
    @Autowired
    IPlatformOrderContentService platformOrderContentService;
    @Autowired
    ISkuDeclaredValueService skuDeclaredValueService;
    @Autowired
    FactureDetailMapper factureDetailMapper;

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
    private String INVOICE_DIR;

    @Value("${jeecg.path.shippingInvoiceDetailDir}")
    private String INVOICE_DETAIL_DIR;

    private final static String[] titles = {
            "Boutique",
            "N° de Mabang",
            "N° de commande",
            "N° de suivi",
            "Date de commande",
            "Date d'expédition",
            "Nom de client",
            "Pays",
            "Code postal",
            "SKU",
            "Nom produits",
            "Quantité",
            "Frais de FRET",
            "Frais de livraison",
            "Frais de service",
            "TVA",
            "N° de facture"
    };

    public Period getValidPeriod(List<String> shopIDs) {
        Date begin = platformOrderMapper.findEarliestUninvoicedPlatformOrder(shopIDs);
        Date end = platformOrderMapper.findLatestUninvoicedPlatformOrder(shopIDs);
        return new Period(begin, end);
    }

    /**
     * Make a pre-shipping invoice for specified orders
     *
     * @param param the parameters to make the invoice
     * @return name of the invoice, can be used to in {@code getInvoiceBinary}.
     * @throws UserException  exception due to error of user input, message will contain detail
     * @throws ParseException exception because of format of "start" and "end" date does not follow
     *                        pattern: "yyyy-MM-dd"
     * @throws IOException    exception related to invoice file IO.
     */
    @Transactional
    public InvoiceMetaData makeInvoice(PreShippingInvoiceParam param) throws UserException, ParseException, IOException {
        // Creates factory
        ShippingInvoiceFactory factory = new ShippingInvoiceFactory(
                platformOrderService,
                clientMapper,
                shopMapper,
                logisticChannelPriceMapper,
                platformOrderContentService,
                skuDeclaredValueService,
                countryService,
                exchangeRatesMapper);
        // Creates invoice by factory
        ShippingInvoice invoice = factory.createPreShippingInvoice(param.clientID(), param.orderIds());
        return getInvoiceMetaData(invoice);
    }

    @NotNull
    private InvoiceMetaData getInvoiceMetaData(ShippingInvoice invoice) throws IOException {
        // Chooses invoice template based on client's preference on currency
        Path src;
        if (invoice.client().getCurrency().equals("USD")) {
            src = Paths.get(INVOICE_TEMPLATE_US);
        } else {
            src = Paths.get(INVOICE_TEMPLATE_EU);
        }
        // Writes invoice content to a new excel file
        String filename = "Invoice N°" + invoice.code() + " (" + invoice.client().getInvoiceEntity() + ").xlsx";
        Path out = Paths.get(INVOICE_DIR, filename);
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
        return new InvoiceMetaData(filename, invoice.code());
    }

    /**
     * Make an invoice based on parameters.
     *
     * @param param the parameters to make the invoice
     * @return identifiant name of the invoice, can be used to in {@code getInvoiceBinary}.
     * @throws UserException  exception due to error of user input, message will contain detail
     * @throws ParseException exception because of format of "start" and "end" date does not follow
     *                        pattern: "yyyy-MM-dd"
     * @throws IOException    exception related to invoice file IO.
     */
    @Transactional
    public InvoiceMetaData makeInvoice(ShippingInvoiceParam param) throws UserException, ParseException, IOException {
        // Creates factory
        ShippingInvoiceFactory factory = new ShippingInvoiceFactory(
                platformOrderService,
                clientMapper,
                shopMapper,
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
        return getInvoiceMetaData(invoice);
    }

    /**
     * Returns byte stream of a invoice file
     *
     * @param filename identifiant name of the invoice file
     * @return byte array of the file
     * @throws IOException error when reading file
     */
    public byte[] getInvoiceBinary(String filename) throws IOException {
        Path out = Paths.get(INVOICE_DIR, filename);
        return Files.readAllBytes(out);
    }

    public List<FactureDetail> getInvoiceDetail(String invoiceNumber) {

        QueryWrapper<FactureDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("`N° de facture`", invoiceNumber);

        return factureDetailMapper.selectList(queryWrapper);
    }

    public byte[] exportToExcel(List<FactureDetail> details, String invoiceNumber) throws IOException {
        SheetManager sheetManager = SheetManager.createXLSX();
        for (String title : titles) {
            sheetManager.write(title);
            sheetManager.nextCol();
        }
        sheetManager.moveCol(0);
        sheetManager.nextRow();

        for (FactureDetail detail : details) {
            sheetManager.write(detail.getBoutique());
            sheetManager.nextCol();
            sheetManager.write(detail.getMabangNum());
            sheetManager.nextCol();
            sheetManager.write(detail.getCommandeNum());
            sheetManager.nextCol();
            sheetManager.write(detail.getSuiviNum());
            sheetManager.nextCol();
            sheetManager.write(detail.getCommandeDate());
            sheetManager.nextCol();
            sheetManager.write(detail.getExpeditionDate());
            sheetManager.nextCol();
            sheetManager.write(detail.getClientName());
            sheetManager.nextCol();
            sheetManager.write(detail.getCountry());
            sheetManager.nextCol();
            sheetManager.write(detail.getPostalCode());
            sheetManager.nextCol();
            sheetManager.write(detail.getSku());
            sheetManager.nextCol();
            sheetManager.write(detail.getProductName());
            sheetManager.nextCol();
            sheetManager.write(detail.getQuantity());
            sheetManager.nextCol();
            sheetManager.write(detail.getFretFee());
            sheetManager.nextCol();
            sheetManager.write(detail.getLivraisonFee());
            sheetManager.nextCol();
            sheetManager.write(detail.getServiceFee());
            sheetManager.nextCol();
            sheetManager.write(detail.getTVA());
            sheetManager.nextCol();
            sheetManager.write(detail.getFactureNum());
            sheetManager.moveCol(0);
            sheetManager.nextRow();
        }

        Path target = Paths.get(INVOICE_DETAIL_DIR, "Détail_calcul_de_facture_" + invoiceNumber + ".xlsx");
        int i = 2;
        while (Files.exists(target)) {
            target = Paths.get(INVOICE_DETAIL_DIR, "Détail_calcul_de_facture_" + invoiceNumber + "_" + i + ".xlsx");
            i++;
        }
        Files.createFile(target);
        sheetManager.export(target);
        return Files.readAllBytes(target);
    }
}