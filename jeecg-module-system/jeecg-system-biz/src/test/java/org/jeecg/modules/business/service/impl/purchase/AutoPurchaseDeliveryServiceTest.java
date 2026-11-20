package org.jeecg.modules.business.service.impl.purchase;

import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.AutoPurchaseMapper;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import javax.mail.MessagingException;
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
    private final AutoPurchaseDeliveryService service = new AutoPurchaseDeliveryService(runs, purchases, providers, emails, mock(Environment.class), details);
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
        return run;
    }
    @Test void sendsAttachmentToConfiguredRecipientAndRecordsPlanSuccess() throws Exception {
        AutoPurchaseRun run = setup(); Responses response = new Responses(); response.addSuccess("group1");
        when(providers.addAutomaticPurchaseOrderToMabang(anyMap(), any(), any(), eq("buyer"))).thenReturn(response);
        service.deliver(run);
        verify(purchases).makeAutomaticInvoice("purchase", "client");
        verify(purchases, never()).makeInvoice(anyString());
        verify(emails).sendMessageWithAttachments(eq("configured@example.com"), contains("PI1"), anyString(),
                eq(Arrays.asList(Paths.get("invoices", "PI1.xlsx").toString(), Paths.get("invoices", "PI1_purchase_details.xlsx").toString())), any());
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
        verifyNoInteractions(emails, details); verify(runs).finishPlan(eq("run"), eq("UNKNOWN"), contains("timeout"));
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
}
