package org.jeecg.modules.business.service.impl.purchase;

import org.apache.poi.ss.usermodel.*;
import org.jeecg.modules.business.domain.purchase.invoice.PurchaseInvoiceEntry;
import org.jeecg.modules.business.mapper.PurchaseOrderContentMapper;
import org.jeecg.modules.business.vo.InvoiceMetaData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseDetailExportServiceTest {
    @TempDir Path directory;
    private final PurchaseOrderContentMapper contents = mock(PurchaseOrderContentMapper.class);
    private final InvoiceMetaData meta = new InvoiceMetaData("Invoice PI1.xlsx", "PI1", "A", "Entity", "");
    private PurchaseDetailExportService service() {
        PurchaseDetailExportService service = new PurchaseDetailExportService(contents);
        ReflectionTestUtils.setField(service, "invoiceDir", directory.toString());
        return service;
    }
    @Test void exportsSavedQuantitiesAndAmountsAsReadableWorkbook() throws Exception {
        when(contents.selectInvoiceDataByID("purchase")).thenReturn(Collections.singletonList(
                new PurchaseInvoiceEntry("=SKU-001", "Product name", 3, new BigDecimal("10.00"))));
        Path path = service().export("purchase", meta);
        assertEquals("Invoice PI1_purchase_details.xlsx", path.getFileName().toString());
        try (Workbook workbook = WorkbookFactory.create(path.toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Quantity", sheet.getRow(0).getCell(3).getStringCellValue());
            assertEquals("PI1", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("=SKU-001", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(CellType.STRING, sheet.getRow(1).getCell(1).getCellType());
            assertEquals(3, sheet.getRow(1).getCell(3).getNumericCellValue());
            assertEquals(3.333333, sheet.getRow(1).getCell(4).getNumericCellValue(), 0.000001);
            assertEquals(10, sheet.getRow(1).getCell(5).getNumericCellValue());
        }
    }
    @Test void refusesEmptyOrInvalidSavedDetails() {
        when(contents.selectInvoiceDataByID("purchase")).thenReturn(Collections.emptyList());
        assertThrows(IllegalStateException.class, () -> service().export("purchase", meta));
        when(contents.selectInvoiceDataByID("purchase")).thenReturn(Collections.singletonList(
                new PurchaseInvoiceEntry("SKU", "Product", 0, BigDecimal.TEN)));
        assertThrows(IllegalStateException.class, () -> service().export("purchase", meta));
        assertFalse(Files.exists(directory.resolve("Invoice PI1_purchase_details.xlsx")));
    }
}
