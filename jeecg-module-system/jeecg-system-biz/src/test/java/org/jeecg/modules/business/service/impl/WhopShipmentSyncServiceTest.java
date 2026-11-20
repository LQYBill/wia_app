package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.api.whop.WhopClient;
import org.jeecg.modules.business.domain.api.whop.WhopShipmentCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WhopShipmentSyncServiceTest {
    private final WhopShipmentSyncRepository repository = mock(WhopShipmentSyncRepository.class);
    private final WhopClient client = mock(WhopClient.class);
    private final WhopShipmentSyncService service = new WhopShipmentSyncService(repository, client);
    private final WhopShipmentCandidate candidate = new WhopShipmentCandidate("pay_test", "TRACK123", 1);

    @BeforeEach void candidates() {
        when(repository.candidates("biz_test", "TID-Whop", 100)).thenReturn(List.of(candidate));
    }

    @Test void persistsBeforePostingAndConfirmsTheReturnedIdentity() {
        JSONObject shipment = shipment("TRACK123");
        when(client.createShipment(eq("biz_test"), eq("pay_test"), eq("TRACK123"), anyString())).thenReturn(shipment);
        assertEquals(1, run());
        org.mockito.InOrder order = inOrder(repository, client);
        order.verify(repository).begin(eq("biz_test"), eq(candidate), anyString());
        order.verify(client).shipmentForPayment("pay_test");
        order.verify(client).createShipment(eq("biz_test"), eq("pay_test"), eq("TRACK123"), anyString());
        order.verify(repository).synced("biz_test", "pay_test", shipment);
    }

    @Test void existingMatchingShipmentIsConfirmedWithoutPosting() {
        when(client.shipmentForPayment("pay_test")).thenReturn(shipment("TRACK123"));
        assertEquals(1, run());
        verify(client, never()).createShipment(anyString(), anyString(), anyString(), anyString());
    }

    @Test void differentExistingTrackingIsRecordedAsConflict() {
        JSONObject remote = shipment("OTHER123");
        when(client.shipmentForPayment("pay_test")).thenReturn(remote);
        assertEquals(0, run());
        verify(repository).failed(eq("biz_test"), eq("pay_test"), eq("CONFLICT"), anyString(), eq(remote));
        verify(client, never()).createShipment(anyString(), anyString(), anyString(), anyString());
    }

    @Test void timedOutCreationIsReconciledOnNextRun() {
        when(client.shipmentForPayment("pay_test")).thenReturn(null).thenReturn(shipment("TRACK123"));
        when(client.createShipment(eq("biz_test"), eq("pay_test"), eq("TRACK123"), anyString()))
                .thenThrow(new ResourceAccessException("timeout"));
        assertEquals(0, run());
        assertEquals(1, run());
        verify(client, times(1)).createShipment(eq("biz_test"), eq("pay_test"), eq("TRACK123"), anyString());
        verify(repository, times(2)).begin("biz_test", candidate,
                WhopShipmentSyncService.idempotencyKey("biz_test", "pay_test", "TRACK123"));
    }

    @Test void inFlightConflictIsReconciledByReadingTheShipment() {
        when(client.shipmentForPayment("pay_test")).thenReturn(null).thenReturn(shipment("TRACK123"));
        when(client.createShipment(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null));
        assertEquals(1, run());
        verify(client, times(2)).shipmentForPayment("pay_test");
    }

    @Test void crossAccountResponseIsNeverMarkedSynced() {
        JSONObject remote = shipment("TRACK123");
        remote.put("account_id", "biz_other");
        when(client.shipmentForPayment("pay_test")).thenReturn(remote);
        assertEquals(0, run());
        verify(repository, never()).synced(anyString(), anyString(), any());
        verify(client, never()).createShipment(anyString(), anyString(), anyString(), anyString());
    }

    @Test void splitOrdersWithDifferentNumbersAreBlockedBeforeAnyApiCall() {
        when(repository.candidates("biz_test", "TID-Whop", 100))
                .thenReturn(List.of(new WhopShipmentCandidate("pay_test", "TRACK123", 2)));
        assertEquals(0, run());
        verify(repository).failed(eq("biz_test"), eq("pay_test"), eq("CONFLICT"), anyString(), isNull());
        verifyNoInteractions(client);
    }

    @Test void dryRunOnlyReadsCandidates() {
        assertEquals(0, service.sync("biz_test", "TID-Whop", 100, true));
        verify(repository, never()).begin(anyString(), any(), anyString());
        verifyNoInteractions(client);
    }

    @Test void permissionFailureIsRecordedAndStopsTheBatch() {
        when(client.shipmentForPayment("pay_test")).thenThrow(
                HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null));
        assertThrows(HttpClientErrorException.class, this::run);
        verify(repository).failed("biz_test", "pay_test", "FAILED", "Whop HTTP 403", null);
        verify(client, never()).createShipment(anyString(), anyString(), anyString(), anyString());
    }

    @Test void idempotencyKeysAreStableAndChangeWithTheLogicalRequest() {
        String key = WhopShipmentSyncService.idempotencyKey("biz_test", "pay_test", "TRACK123");
        assertEquals(key, WhopShipmentSyncService.idempotencyKey("biz_test", "pay_test", "TRACK123"));
        assertNotEquals(key, WhopShipmentSyncService.idempotencyKey("biz_test", "pay_test", "OTHER123"));
        assertNotEquals(key, WhopShipmentSyncService.idempotencyKey("biz_other", "pay_test", "TRACK123"));
    }

    private int run() { return service.sync("biz_test", "TID-Whop", 100, false); }

    private JSONObject shipment(String number) {
        JSONObject result = JSON.parseObject("{\"id\":\"ship_test\",\"account_id\":\"biz_test\",\"payment_id\":\"pay_test\"}");
        result.put("tracking_number", number);
        return result;
    }
}
