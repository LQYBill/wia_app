package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.domain.api.mabang.dochangeorder.EditRemarkRequestBody;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.Order;
import org.jeecg.modules.business.vo.ResponsesWithMsg;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceOrderRemarkTest {
    private static final String INVOICE = "2026-11-7001";

    private Order order(String id, String remark) {
        Order order = new Order();
        order.setPlatformOrderId(id);
        order.setRemark(remark);
        return order;
    }

    @Test
    void skipsExistingInvoiceMarkersAndPreservesOtherRemarks() {
        List<Order> orders = Arrays.asList(order("a", INVOICE), order("b", "note [" + INVOICE + "]"),
                order("c", "older 2026-10-7001"));
        ResponsesWithMsg<String> responses = new ResponsesWithMsg<>();
        List<EditRemarkRequestBody> requests = PlatformOrderMabangServiceImpl.invoiceRemarkRequests(
                INVOICE, Arrays.asList("a", "b", "c"), orders, responses);
        assertEquals(1, requests.size());
        assertEquals("c", requests.get(0).getPlatformOrderId());
        assertEquals("older 2026-10-7001 " + INVOICE, requests.get(0).getRemark());
        assertTrue(responses.getSuccesses().containsKey("a"));
        assertTrue(responses.getSuccesses().containsKey("b"));
    }

    @Test
    void handlesEmptyRemarksAndDuplicateOrderIdsWithoutDuplicateWrites() {
        ResponsesWithMsg<String> responses = new ResponsesWithMsg<>();
        List<EditRemarkRequestBody> requests = PlatformOrderMabangServiceImpl.invoiceRemarkRequests(
                INVOICE, Arrays.asList("a", "a", "b", "c"),
                Arrays.asList(order("a", null), order("a", null), order("b", ""), order("c", "  ")), responses);
        assertEquals(3, requests.size());
        requests.forEach(request -> assertEquals(INVOICE, request.getRemark()));
    }

    @Test
    void doesNotMistakeLongerInvoiceNumberForExistingMarker() {
        List<EditRemarkRequestBody> requests = PlatformOrderMabangServiceImpl.invoiceRemarkRequests(
                INVOICE, Collections.singletonList("a"), Collections.singletonList(order("a", INVOICE + "0")),
                new ResponsesWithMsg<>());
        assertEquals(1, requests.size());
    }

    @Test
    void repeatedRunProducesNoWrites() {
        ResponsesWithMsg<String> responses = new ResponsesWithMsg<>();
        List<EditRemarkRequestBody> first = PlatformOrderMabangServiceImpl.invoiceRemarkRequests(
                INVOICE, Collections.singletonList("a"), Collections.singletonList(order("a", "note")), responses);
        List<EditRemarkRequestBody> second = PlatformOrderMabangServiceImpl.invoiceRemarkRequests(
                INVOICE, Collections.singletonList("a"), Collections.singletonList(order("a", first.get(0).getRemark())),
                new ResponsesWithMsg<>());
        assertTrue(second.isEmpty());
    }

    @Test
    void reportsUnreadOrdersInsteadOfSilentlySucceeding() {
        ResponsesWithMsg<String> responses = new ResponsesWithMsg<>();
        assertTrue(PlatformOrderMabangServiceImpl.invoiceRemarkRequests(INVOICE,
                Collections.singletonList("missing"), Collections.emptyList(), responses).isEmpty());
        assertTrue(responses.getFailures().containsKey("missing"));
    }
}
