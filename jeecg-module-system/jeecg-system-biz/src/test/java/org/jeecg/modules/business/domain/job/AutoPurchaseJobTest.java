package org.jeecg.modules.business.domain.job;

import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.impl.purchase.AutoPurchasePreparationService;
import org.jeecg.modules.business.service.impl.purchase.AutoPurchaseDeliveryService;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AutoPurchaseJobTest {
    private static final String TWO_CLIENTS = "{\"clients\":{"
            + "\"A\":{\"email\":\"a@example.com\",\"mabangUsername\":\"buyer_a\",\"autoMarkPaid\":true},"
            + "\"B\":{\"email\":\"b@example.com\",\"mabangUsername\":\"buyer_b\",\"autoMarkPaid\":false}}}";

    @Test void requiresExplicitClientsAndEachClientsMabangAccount() {
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse(null));
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse("{}"));
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse("{\"clients\":{}}"));
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse("{\"clients\":{\"A\":{\"email\":\"a@example.com\"}}}"));
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse(TWO_CLIENTS.replace("buyer_b", " ")));
    }
    @Test void rejectsInvalidEmailAndNonBooleanPaymentSwitch() {
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse(TWO_CLIENTS.replace("a@example.com", "bad")));
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse(TWO_CLIENTS.replace("\"autoMarkPaid\":true", "\"autoMarkPaid\":\"true\"")));
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse(TWO_CLIENTS.replace("\"autoMarkPaid\":false", "\"autoMarkPaid\":null")));
    }
    @Test void defaultsToUnpaidAndParisDateBoundary() throws Exception {
        AutoPurchaseJob.Parameters result = AutoPurchaseJob.parse(TWO_CLIENTS.replace(",\"autoMarkPaid\":false", ""));
        assertTrue(result.clients.get("A").autoMarkPaid);
        assertFalse(result.clients.get("B").autoMarkPaid);
        assertEquals("Europe/Paris", result.zone.getId());
    }
    @Test void keepsRecipientsBuyersAndPaymentSwitchesIndependent() throws Exception {
        AutoPurchaseJob.Parameters result = AutoPurchaseJob.parse(TWO_CLIENTS);
        assertEquals("a@example.com", result.clients.get("A").email);
        assertEquals("buyer_a", result.clients.get("A").mabangUsername);
        assertTrue(result.clients.get("A").autoMarkPaid);
        assertEquals("b@example.com", result.clients.get("B").email);
        assertEquals("buyer_b", result.clients.get("B").mabangUsername);
        assertFalse(result.clients.get("B").autoMarkPaid);
    }
    @Test void rejectsGlobalSettingsInsteadOfSilentlyApplyingThemToClients() {
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse("{\"clientEmails\":{\"A\":\"a@example.com\"},\"mabangUsername\":\"buyer\"}"));
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse("{\"autoMarkPaid\":true," + TWO_CLIENTS.substring(1)));
        assertThrows(Exception.class, () -> AutoPurchaseJob.parse("{\"mabangUsername\":\"global\"," + TWO_CLIENTS.substring(1)));
    }
    @Test void executesEachClientWithItsOwnSettingsRegardlessOfDatabaseOrder() throws Exception {
        IClientService clients = mock(IClientService.class);
        AutoPurchasePreparationService preparation = mock(AutoPurchasePreparationService.class);
        AutoPurchaseDeliveryService delivery = mock(AutoPurchaseDeliveryService.class);
        Client a = new Client(); a.setInternalCode("A");
        Client b = new Client(); b.setInternalCode("B");
        when(clients.getClientsByCodes(anyList())).thenReturn(Arrays.asList(b, a));
        AutoPurchaseJob job = new AutoPurchaseJob();
        ReflectionTestUtils.setField(job, "clients", clients);
        ReflectionTestUtils.setField(job, "preparation", preparation);
        ReflectionTestUtils.setField(job, "delivery", delivery);
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap data = new JobDataMap(); data.put("parameter", TWO_CLIENTS);
        when(context.getMergedJobDataMap()).thenReturn(data);
        job.execute(context);
        verify(preparation).prepare(eq(a), anyString(), eq("a@example.com"), eq(true), eq("buyer_a"));
        verify(preparation).prepare(eq(b), anyString(), eq("b@example.com"), eq(false), eq("buyer_b"));
    }
    @Test void validatesAllClientSettingsBeforeCreatingAnyInvoice() {
        IClientService clients = mock(IClientService.class);
        AutoPurchasePreparationService preparation = mock(AutoPurchasePreparationService.class);
        AutoPurchaseJob job = new AutoPurchaseJob();
        ReflectionTestUtils.setField(job, "clients", clients);
        ReflectionTestUtils.setField(job, "preparation", preparation);
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap data = new JobDataMap(); data.put("parameter", TWO_CLIENTS.replace("buyer_b", ""));
        when(context.getMergedJobDataMap()).thenReturn(data);
        assertThrows(JobExecutionException.class, () -> job.execute(context));
        verifyNoInteractions(clients, preparation);
    }
}
