package org.jeecg.modules.business.domain;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.jeecg.modules.business.domain.codeGeneration.InvoiceRef;
import org.jeecg.modules.business.domain.codeGeneration.PurchaseInvoiceCodeRule;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.vo.PromotionDetail;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

/**
 * Control content of purchase invoice
 */
public class PurchaseInvoice {
    private final Client targetClient;

    private final String code;

    private final List<PurchaseInvoiceEntry> purchaseInvoiceEntries;

    private final List<PromotionDetail> promotions;

    public PurchaseInvoice(Client targetClient,
                           String code,
                           List<PurchaseInvoiceEntry> purchaseInvoiceEntries,
                           List<PromotionDetail> promotions) {
        this.targetClient = targetClient;
        this.code = code;
        this.purchaseInvoiceEntries = purchaseInvoiceEntries;
        this.promotions = promotions;
    }

    /* constants of cell location */
    private final static String INVOICE_CODE_LOCATION = "G3";
    private final static String GENERATED_DATE_LOCATION = "G4";
    private final static String ENTITY_NAME_LOCATION = "F7";
    private final static String INVOICE_ADR_LINE1 = "F9";
    private final static String INVOICE_ADR_LINE2 = "F10";
    private final static String PHONE_LOCATION = "F12";
    private final static String MAIL_LOCATION = "F13";
    private final static String SUBJECT_LOCATION = "C16";
    private final static String[] NUM_COL = {"C", "D", "E", "F", "G", "H"};
    private final static int FIRST_ROW = 20;
    private final static int LAST_ROW = 39;


    /**
     * Export content to the excel file; indicated by path out.
     *
     * @param out the excel file to export, extension: "xlsx"
     */
    public void toExcelFile(Path out) {
        ExcelWriter writer = ExcelUtil.getWriter(out.toFile(), "FACTURE");
        InvoiceStyleFactory factory = new InvoiceStyleFactory(writer.getWorkbook());

        for (Map.Entry<String, Object> entry : invoiceInformation().entrySet()) {
            writer.writeCellValue(entry.getKey(), entry.getValue());
            writer.setStyle(factory.otherStyle(), entry.getKey());
        }
        // table section
        for (Map.Entry<String, Object> entry : tableData().entrySet()) {
            writer.writeCellValue(entry.getKey(), entry.getValue());
            if (entry.getKey().charAt(0) <= 'D')
                writer.setStyle(factory.leftSideStyle(), entry.getKey());
            else
                writer.setStyle(factory.rightSideStyle(), entry.getKey());
        }
        writer.setStyle(factory.invoiceCodeStyle(), INVOICE_CODE_LOCATION);
        // revaluate formulae to enable automatic calculation
        FormulaEvaluator evaluator = writer.getWorkbook().getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
        writer.flush();
        writer.close();
    }

    /**
     * Fill content that does not belong to the table section.
     *
     * @return a map between a cell location and its contents. For example: G3 -> Toto
     */
    private Map<String, Object> invoiceInformation() {
        Map<String, Object> cellContentMap = new HashMap<>();
        cellContentMap.put(INVOICE_CODE_LOCATION, code);
        cellContentMap.put(GENERATED_DATE_LOCATION, InvoiceStyleFactory.INVOICE_CODE_DATETIME_FORMAT.format(new Date()));
        cellContentMap.put(ENTITY_NAME_LOCATION, targetClient.fullName());
        cellContentMap.put(
                INVOICE_ADR_LINE1,
                String.format("%s %s, %s",
                        targetClient.getStreetNumber(),
                        targetClient.getStreetName(),
                        targetClient.getPostcode()
                )
        );
        cellContentMap.put(
                INVOICE_ADR_LINE2,
                String.format("%s, %s",
                        targetClient.getCity(),
                        targetClient.getCountry()
                )
        );
        cellContentMap.put(PHONE_LOCATION, targetClient.getPhone());
        cellContentMap.put(MAIL_LOCATION, targetClient.getEmail());
        return cellContentMap;
    }

    /**
     * Fill cells that belong to the table section.
     *
     * @return map between location and table cell
     */
    private Map<String, Object> tableData() {
        Map<String, Object> cellContentMap = new HashMap<>();

        InvoiceRef refRule = new InvoiceRef();

        int row = FIRST_ROW;
        for (PurchaseInvoiceEntry entry : purchaseInvoiceEntries) {
            String ref = refRule.next(String.valueOf(row - FIRST_ROW + 1));
            cellContentMap.put(NUM_COL[0] + row, ref);

            String desc = entry.getSku_en_name();
            cellContentMap.put(NUM_COL[1] + row, desc);

            BigDecimal unitPrice = entry.unitPrice();
            cellContentMap.put(NUM_COL[2] + row, unitPrice);

            int quantity = entry.getQuantity();
            cellContentMap.put(NUM_COL[3] + row, quantity);

            BigDecimal totalAmount = entry.getTotalAmount();
            cellContentMap.put(NUM_COL[5] + row, totalAmount);

            row++;
        }

        for (PromotionDetail pd : promotions) {
            String ref = refRule.next(String.valueOf(row - FIRST_ROW + 1));
            cellContentMap.put(NUM_COL[0] + row, ref);

            String desc = String.format("Promotion: %s", pd.getName());
            cellContentMap.put(NUM_COL[1] + row, desc);

            BigDecimal totalAmount = pd.getUnitAmount()
                    .multiply(BigDecimal.valueOf(pd.getCount()));
            cellContentMap.put(NUM_COL[4] + row, totalAmount);
            row++;
        }
        return cellContentMap;
    }


}

