package org.jeecg.modules.business.service.impl.purchase;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jeecg.modules.business.domain.purchase.invoice.PurchaseInvoiceEntry;
import org.jeecg.modules.business.mapper.PurchaseOrderContentMapper;
import org.jeecg.modules.business.vo.InvoiceMetaData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.util.List;

/** Exports saved purchase lines; no current price or inventory recalculation. */
@Service
public class PurchaseDetailExportService {
    private final PurchaseOrderContentMapper contents;
    @Value("${jeecg.path.purchaseInvoiceDir}") private String invoiceDir;

    public PurchaseDetailExportService(PurchaseOrderContentMapper contents) { this.contents = contents; }

    public Path export(String purchaseId, InvoiceMetaData meta) throws Exception {
        List<PurchaseInvoiceEntry> entries = contents.selectInvoiceDataByID(purchaseId);
        if (entries == null || entries.isEmpty()) throw new IllegalStateException("Purchase details are empty: " + meta.getInvoiceCode());
        String filename = meta.getFilename().replaceFirst("(?i)\\.xlsx$", "") + "_purchase_details.xlsx";
        Path target = Paths.get(invoiceDir, filename);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Purchase details");
            String[] titles = {"Invoice number", "SKU", "Product", "Quantity", "Unit price (EUR)", "Amount before promotions (EUR)"};
            CellStyle heading = workbook.createCellStyle();
            Font font = workbook.createFont(); font.setBold(true); heading.setFont(font);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("0.00####"));
            Row header = sheet.createRow(0);
            for (int column = 0; column < titles.length; column++) {
                Cell cell = header.createCell(column); cell.setCellValue(titles[column]); cell.setCellStyle(heading);
            }
            int rowNumber = 1;
            for (PurchaseInvoiceEntry entry : entries) {
                if (entry.getQuantity() == null || entry.getQuantity() <= 0 || entry.getTotalAmount() == null) {
                    throw new IllegalStateException("Invalid saved purchase detail for " + entry.getErp_code());
                }
                Row row = sheet.createRow(rowNumber++);
                row.createCell(0).setCellValue(meta.getInvoiceCode());
                row.createCell(1).setCellValue(entry.getErp_code());
                row.createCell(2).setCellValue(entry.getSku_en_name());
                row.createCell(3).setCellValue(entry.getQuantity());
                row.createCell(4).setCellValue(entry.getTotalAmount().divide(BigDecimal.valueOf(entry.getQuantity()), 6, RoundingMode.HALF_UP).doubleValue());
                row.createCell(5).setCellValue(entry.getTotalAmount().doubleValue());
                row.getCell(4).setCellStyle(money); row.getCell(5).setCellStyle(money);
            }
            int[] widths = {24, 28, 50, 14, 24, 38};
            for (int column = 0; column < widths.length; column++) sheet.setColumnWidth(column, widths[column] * 256);
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, rowNumber - 1, 0, titles.length - 1));
            try (OutputStream output = Files.newOutputStream(target)) { workbook.write(output); }
        }
        return target;
    }
}
