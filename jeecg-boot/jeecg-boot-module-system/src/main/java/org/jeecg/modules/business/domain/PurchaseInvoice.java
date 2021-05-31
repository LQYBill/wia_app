package org.jeecg.modules.business.domain;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.jeecg.modules.business.entity.Client;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

public class PurchaseInvoice {
    private final Client targetClient;

    private final String code;

    private final List<PurchaseInvoiceEntry> purchaseInvoiceEntries;

    public PurchaseInvoice(Client targetClient, String code, List<PurchaseInvoiceEntry> purchaseInvoiceEntries) {
        this.targetClient = targetClient;
        this.code = code;
        this.purchaseInvoiceEntries = purchaseInvoiceEntries;
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


    public void toExcelFile(Path out) {
        ExcelWriter writer = ExcelUtil.getWriter(out.toFile(), "FACTURE");
        Map<String, Object> data = new HashMap<>(invoiceInformation());
        data.putAll(tableData());
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            writer.writeCellValue(entry.getKey(), entry.getValue());
        }
        // revaluate formulae to enable automatic calculation
        FormulaEvaluator evaluator = writer.getWorkbook().getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
        writer.flush();
        writer.close();
    }

    private Map<String, Object> invoiceInformation() {
        Map<String, Object> cellContentMap = new HashMap<>();
        cellContentMap.put(INVOICE_CODE_LOCATION, code);
        cellContentMap.put(GENERATED_DATE_LOCATION, new Date().toString());
        cellContentMap.put(ENTITY_NAME_LOCATION, targetClient.fullName());
        cellContentMap.put(
                INVOICE_ADR_LINE1,
                String.format("%s %s %s",
                        targetClient.getStreetNumber(),
                        targetClient.getStreetName(),
                        targetClient.getPostcode()
                )
        );
        cellContentMap.put(
                INVOICE_ADR_LINE2,
                String.format("%s %s",
                        targetClient.getCity(),
                        targetClient.getCountry()
                )
        );
        cellContentMap.put(PHONE_LOCATION, targetClient.getPhone());
        cellContentMap.put(MAIL_LOCATION, targetClient.getEmail());
        return cellContentMap;
    }

    private Map<String, Object> tableData() {
        Map<String, Object> cellContentMap = new HashMap<>();

        int row = FIRST_ROW;
        for (PurchaseInvoiceEntry entry : purchaseInvoiceEntries) {
            int ref = row - FIRST_ROW + 1;
            cellContentMap.put(NUM_COL[0] + row, ref);

            String desc = entry.getSku_en_name();
            cellContentMap.put(NUM_COL[1] + row, desc);

            BigDecimal unitPrice = entry.getUnitPrice();
            cellContentMap.put(NUM_COL[2] + row, unitPrice);

            int quantity = entry.getQuantity();
            cellContentMap.put(NUM_COL[3] + row, quantity);

            BigDecimal totalAmount = entry.totalAmount();
            cellContentMap.put(NUM_COL[5] + row, totalAmount);

            if (entry.hasPromotion()) {
                row++;
                ref = row - FIRST_ROW + 1;
                cellContentMap.put(NUM_COL[0] + row, ref);

                desc = String.format("Promotion for sku %s", entry.getSku_en_name());
                cellContentMap.put(NUM_COL[1] + row, desc);

                totalAmount = entry.getPromotionAmount()
                        .multiply(BigDecimal.valueOf(entry.getPromotionCount()));
                cellContentMap.put(NUM_COL[4] + row, totalAmount);
            }
            row++;
        }
        return cellContentMap;

    }
}
