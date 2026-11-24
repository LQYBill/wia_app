package org.jeecg.modules.business.service.impl.purchase;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.AutoPurchaseMapper;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AutoPurchaseDeliveryServiceTest {
    private final AutoPurchaseMapper runs = mock(AutoPurchaseMapper.class);
    private final IPurchaseOrderService purchases = mock(IPurchaseOrderService.class);
    private final IProviderMabangService providers = mock(IProviderMabangService.class);
    private final EmailService emails = mock(EmailService.class);
    private final PurchaseDetailExportService details = mock(PurchaseDetailExportService.class);
    private final PlatformOrderShippingInvoiceService invoiceFiles = mock(PlatformOrderShippingInvoiceService.class);
    private final AutoPurchaseDeliveryService service = new AutoPurchaseDeliveryService(
            runs, purchases, providers, emails, mock(Environment.class), details, invoiceFiles);
    private AutoPurchaseRun setup() throws Exception {
        ReflectionTestUtils.setField(service, "invoiceDir", "invoices");
        AutoPurchaseRun run = new AutoPurchaseRun(); run.setId("run"); run.setPurchaseId("purchase"); run.setClientId("client");
        run.setRecipient("configured@example.com"); run.setMabangUsername("buyer"); run.setPlanState("READY");
        when(runs.claimPlan("run")).thenReturn(1);
        PurchaseOrder order = new PurchaseOrder(); order.setStatus(1);
        when(purchases.getById("purchase")).thenReturn(order);
        when(purchases.makeAutomaticInvoice("purchase", "client")).thenReturn(new InvoiceMetaData("PI1.xlsx", "PI1", "A", "Entity", ""));
        when(purchases.getSkuQuantityByInvoiceNumber("PI1")).thenReturn(Collections.singletonList(new SkuQuantity("sku", "ERP", 7)));
        when(emails.getMailSender()).thenReturn(new Properties());
        when(details.export(eq("purchase"), any())).thenReturn(Paths.get("invoices", "PI1_purchase_details.xlsx"));
        when(invoiceFiles.convertToPdf(Paths.get("invoices", "PI1.xlsx"))).thenReturn(Paths.get("pdf", "PI1.pdf"));
        return run;
    }
    @Test void sendsAttachmentToConfiguredRecipientAndRecordsPlanSuccess() throws Exception {
        AutoPurchaseRun run = setup(); Responses response = new Responses(); response.addSuccess("group1");
        when(providers.addAutomaticPurchaseOrderToMabang(anyMap(), any(), any(), eq("buyer"))).thenReturn(response);
        service.deliver(run);
        verify(purchases).makeAutomaticInvoice("purchase", "client");
        verify(purchases, never()).makeInvoice(anyString());
        verify(emails).sendMessageWithAttachments(eq("configured@example.com"), contains("PI1"), anyString(),
                eq(Arrays.asList(Paths.get("pdf", "PI1.pdf").toString(), Paths.get("invoices", "PI1_purchase_details.xlsx").toString())), any());
        verify(runs).emailSent("run"); verify(runs).finishPlan(eq("run"), eq("DONE"), contains("group1"));
    }
    @Test void mailFailureCanRetryWithoutSubmittingMabangPlan() throws Exception {
        AutoPurchaseRun run = setup();
        doThrow(new MessagingException("smtp unavailable")).when(emails).sendMessageWithAttachments(anyString(), anyString(), anyString(), anyList(), any());
        assertThrows(MessagingException.class, () -> service.deliver(run));
        verifyNoInteractions(providers); verify(runs).finishPlan(eq("run"), eq("READY"), contains("smtp unavailable"));
    }
    @Test void ambiguousMabangResultRequiresReconciliationAndDoesNotResendMail() throws Exception {
        AutoPurchaseRun run = setup(); run.setEmailSent(true);
        when(providers.addAutomaticPurchaseOrderToMabang(anyMap(), any(), any(), anyString())).thenThrow(new RuntimeException("timeout"));
        assertThrows(RuntimeException.class, () -> service.deliver(run));
        verifyNoInteractions(emails, details, invoiceFiles); verify(runs).finishPlan(eq("run"), eq("UNKNOWN"), contains("timeout"));
        run.setPlanState("UNKNOWN");
        assertThrows(IllegalStateException.class, () -> service.deliver(run));
        verify(providers, times(1)).addAutomaticPurchaseOrderToMabang(anyMap(), any(), any(), anyString());
    }
    @Test void concurrentDeliveryCannotSubmitTwice() throws Exception {
        AutoPurchaseRun run = setup(); when(runs.claimPlan("run")).thenReturn(0);
        service.deliver(run); verifyNoInteractions(emails, providers);
    }
    @Test void partialSuccessIsPreservedForManualReconciliation() throws Exception {
        AutoPurchaseRun run = setup(); run.setEmailSent(true);
        Responses response = new Responses(); response.addSuccess("group1"); response.addFailure("supplier failed");
        when(providers.addAutomaticPurchaseOrderToMabang(anyMap(), any(), any(), anyString())).thenReturn(response);
        assertThrows(IllegalStateException.class, () -> service.deliver(run));
        verify(runs).finishPlan(eq("run"), eq("UNKNOWN"), contains("group1"));
    }
    @Test void detailGenerationFailureDoesNotSendIncompleteEmailOrSubmitPlan() throws Exception {
        AutoPurchaseRun run = setup();
        when(details.export(eq("purchase"), any())).thenThrow(new java.io.IOException("export failed"));
        assertThrows(java.io.IOException.class, () -> service.deliver(run));
        verify(emails, never()).sendMessageWithAttachments(anyString(), anyString(), anyString(), anyList(), any());
        verify(runs, never()).emailSent(anyString());
        verifyNoInteractions(providers);
        verify(runs).finishPlan(eq("run"), eq("READY"), contains("export failed"));
    }

    @Test void pdfConversionFailureLeavesRunReadyWithoutSendingMailOrSubmittingPlan() throws Exception {
        AutoPurchaseRun run = setup();
        when(invoiceFiles.convertToPdf(any(Path.class))).thenThrow(new IOException("conversion failed"));
        assertThrows(IOException.class, () -> service.deliver(run));
        verifyNoInteractions(emails, details, providers);
        verify(runs, never()).emailSent(anyString());
        verify(runs).finishPlan(eq("run"), eq("READY"), contains("conversion failed"));
    }

    @Test void invoiceWorkbookRendersAsPdf(@TempDir Path directory) throws Exception {
        Path spreadsheet = directory.resolve("Invoice N°PI1 (Entity).xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                var output = Files.newOutputStream(spreadsheet)) {
            workbook.createSheet().createRow(0).createCell(0).setCellValue("Purchase invoice PI1");
            workbook.write(output);
        }

        Path pdfDirectory = directory.resolve("pdf");
        PlatformOrderShippingInvoiceService converter = new PlatformOrderShippingInvoiceService();
        ReflectionTestUtils.setField(converter, "INVOICE_PDF_DIR", pdfDirectory.toString());
        Path rendered = converter.convertToPdf(spreadsheet);
        assertEquals(pdfDirectory.resolve("Invoice N°PI1 (Entity).pdf"), rendered);
        try (var contents = Files.newInputStream(rendered)) {
            assertEquals("%PDF-", new String(contents.readNBytes(5), StandardCharsets.US_ASCII));
        }
    }
}
