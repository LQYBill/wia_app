package org.jeecg.modules.business.service.impl.purchase;

import org.apache.shiro.UnavailableSecurityManagerException;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.PurchaseOrder;
import org.jeecg.modules.business.mapper.PurchaseOrderMapper;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.InvoiceMetaData;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PurchaseInvoiceAccessTest {
    private final PurchaseOrderMapper orders = mock(PurchaseOrderMapper.class);
    private final ISecurityService security = mock(ISecurityService.class);
    private final IClientService clients = mock(IClientService.class);
    private final InvoiceMetaData metadata = new InvoiceMetaData("PI1.xlsx", "PI1", "FC", "Entity", "");
    private boolean rendered;
    private final PurchaseOrderServiceImpl service = new PurchaseOrderServiceImpl() {
        @Override protected InvoiceMetaData writeInvoice(String id, PurchaseOrder order) {
            rendered = true;
            assertEquals("purchase", id); assertEquals("client", order.getClientId());
            return metadata;
        }
    };
    private void setup() {
        ReflectionTestUtils.setField(service, "purchaseOrderMapper", orders);
        ReflectionTestUtils.setField(service, "securityService", security);
        ReflectionTestUtils.setField(service, "clientService", clients);
        PurchaseOrder order = new PurchaseOrder(); order.setId("purchase"); order.setClientId("client");
        when(orders.selectById("purchase")).thenReturn(order);
    }
    @Test void scheduledInvoiceDoesNotRequireWebSecurityManager() throws Exception {
        setup();
        when(security.checkIsEmployee()).thenThrow(new UnavailableSecurityManagerException("No web context"));
        assertSame(metadata, service.makeAutomaticInvoice("purchase", "client"));
        assertTrue(rendered); verifyNoInteractions(security, clients);
    }
    @Test void scheduledInvoiceRejectsDifferentClient() {
        setup();
        assertThrows(UserException.class, () -> service.makeAutomaticInvoice("purchase", "other"));
        assertFalse(rendered); verifyNoInteractions(security, clients);
    }
    @Test void scheduledInvoiceRequiresClient() {
        setup();
        assertThrows(UserException.class, () -> service.makeAutomaticInvoice("purchase", null));
        assertThrows(UserException.class, () -> service.makeAutomaticInvoice("purchase", " "));
        assertFalse(rendered); verifyNoInteractions(security, clients);
    }
    @Test void scheduledInvoiceReportsMissingPurchaseClearly() {
        setup(); when(orders.selectById("purchase")).thenReturn(null);
        assertThrows(UserException.class, () -> service.makeAutomaticInvoice("purchase", "client"));
        assertFalse(rendered); verifyNoInteractions(security, clients);
    }
    @Test void webClientCanStillGenerateOwnInvoice() throws Exception {
        setup(); Client client = new Client(); client.setId("client");
        when(clients.getCurrentClient()).thenReturn(client);
        assertSame(metadata, service.makeInvoice("purchase"));
        verify(security).checkIsEmployee(); assertTrue(rendered);
    }
    @Test void webClientCannotGenerateAnotherClientsInvoice() {
        setup(); Client client = new Client(); client.setId("other");
        when(clients.getCurrentClient()).thenReturn(client);
        assertThrows(UserException.class, () -> service.makeInvoice("purchase"));
        assertFalse(rendered); verify(security).checkIsEmployee();
    }
    @Test void webEmployeeCanStillGenerateInvoice() throws Exception {
        setup(); when(security.checkIsEmployee()).thenReturn(true);
        assertSame(metadata, service.makeInvoice("purchase"));
        assertTrue(rendered); verify(clients, never()).getCurrentClient();
    }
    @Test void webInvoiceDoesNotTreatMissingSecurityContextAsAuthorized() {
        setup(); when(security.checkIsEmployee()).thenThrow(new UnavailableSecurityManagerException("No web context"));
        assertThrows(UnavailableSecurityManagerException.class, () -> service.makeInvoice("purchase"));
        assertFalse(rendered);
    }
    @Test void webInvoiceStillRequiresClientLoginForNonEmployee() {
        setup(); assertThrows(UserException.class, () -> service.makeInvoice("purchase"));
        assertFalse(rendered);
    }
}
