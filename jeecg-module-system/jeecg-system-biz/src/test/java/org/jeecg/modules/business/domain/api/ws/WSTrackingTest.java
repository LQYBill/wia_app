package org.jeecg.modules.business.domain.api.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.business.domain.job.WSJob;
import org.jeecg.modules.business.entity.Parcel;
import org.jeecg.modules.business.entity.ParcelTrace;
import org.jeecg.modules.business.mapper.ParcelMapper;
import org.jeecg.modules.business.mapper.ParcelTraceMapper;
import org.jeecg.modules.business.service.impl.WSParcelService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class WSTrackingTest {
    private final WSTraceNormalizer normalizer = new WSTraceNormalizer(Collections.emptyList());

    @Test
    void neutralizesOriginNamesAndChineseWhileRetainingForeignLocationsAndOutcomes() {
        assertEquals("Received at logistics facility", normalizer.description("仓库已收货"));
        assertEquals("Departed from Origin location, Origin location",
                normalizer.description("Departed from Shenzhen, China"));
        assertEquals("Postal carrier accepted shipment", normalizer.description("China Post accepted shipment"));
        assertEquals("Shipment update", normalizer.description("深圳国际交换局已封发"));
        String longDescription = normalizer.description("Shipment information " + "x".repeat(300));
        assertEquals(256, longDescription.codePointCount(0, longDescription.length()));
        assertTrue(longDescription.endsWith("…"));
        assertEquals("Logistics facility", normalizer.location("广州市"));
        assertEquals("Paris", normalizer.location("Paris"));
        assertEquals("Your shipment has been delivered successfully",
                normalizer.description("Your shipment has been delivered successfully"));
        assertEquals("Delivery attempt unsuccessful", normalizer.description("中国派送失败"));
        assertEquals("Returned to sender", normalizer.description("深圳退件"));
        assertEquals("Origin location", new WSTraceNormalizer(Collections.singletonList("CustomHub"))
                .location("CustomHub"));
    }

    @Test
    void parsesUtcIndependentlyOfDefaultTimeZone() {
        TimeZone saved = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            assertEquals(Instant.parse("2026-09-26T03:03:24Z"),
                    WSParcelService.parseUtc("2026-09-26T03:03:24.000+00:00").toInstant());
            assertEquals(Instant.parse("2026-09-26T03:03:24Z"),
                    WSParcelService.parseUtc("2026-09-26 03:03:24").toInstant());
            assertThrows(RuntimeException.class, () -> WSParcelService.parseUtc("2026-02-30 12:00:00"));
        } finally {
            TimeZone.setDefault(saved);
        }
    }

    @Test
    void onlyMatchingLatestEventInheritsDeliveredStatus() throws Exception {
        WSTrackResponse.TrackData data = sample();
        List<ParcelTrace> traces = WSParcelService.convert(data, normalizer);
        assertEquals("Order Placed", traces.get(0).getScanType());
        assertNull(traces.get(0).getTraceStatus());
        assertEquals("End Delivered", traces.get(1).getScanType());
        assertEquals("8", traces.get(1).getTraceStatus());
        assertEquals("End Abnormal", normalizer.scanType("Delivery failed", null));
        assertEquals("End Abnormal", normalizer.scanType("Shipment not delivered", 8));
        assertEquals("Return", normalizer.scanType("Returned to sender", null));
    }

    @Test
    void repeatedSyncUpdatesExistingRowsAndNeutralizesHistoricalText() throws Exception {
        ParcelMapper parcels = mock(ParcelMapper.class);
        ParcelTraceMapper traces = mock(ParcelTraceMapper.class);
        WSParcelService service = service(parcels, traces);
        List<Parcel> storedParcels = new ArrayList<>();
        List<ParcelTrace> storedTraces = new ArrayList<>();
        when(parcels.searchByBillCode(anyList())).thenAnswer(invocation -> new ArrayList<>(storedParcels));
        when(parcels.insert(any(Parcel.class))).thenAnswer(invocation -> {
            Parcel parcel = invocation.getArgument(0);
            parcel.setId("parcel-1");
            storedParcels.add(parcel);
            return 1;
        });
        when(traces.selectByMainId("parcel-1")).thenAnswer(invocation -> new ArrayList<>(storedTraces));
        when(traces.insert(any(ParcelTrace.class))).thenAnswer(invocation -> {
            ParcelTrace trace = invocation.getArgument(0);
            trace.setId("trace-" + storedTraces.size());
            storedTraces.add(trace);
            return 1;
        });
        service.save("WS-INTERNAL-0001", sample(), Collections.emptyList());
        assertEquals(2, storedTraces.size());
        assertEquals("REF-TEST-0001", storedParcels.get(0).getOrderNo());
        assertEquals("FR", storedParcels.get(0).getCountry());
        assertEquals("WS-INTERNAL-0001", storedParcels.get(0).getBillCode());
        assertEquals("WS-TEST-0001", storedParcels.get(0).getThirdBillCode());
        // Simulate a previously stored unsanitized row missing from a later provider snapshot.
        ParcelTrace legacy = new ParcelTrace();
        legacy.setId("legacy");
        legacy.setParcelId("parcel-1");
        legacy.setScanTime(Date.from(Instant.parse("2026-09-24T08:00:00Z")));
        legacy.setDescription("深圳国际交换局已封发");
        legacy.setDescriptionEn("Departed from Shenzhen, China");
        legacy.setTraceLocation("深圳");
        storedTraces.add(legacy);
        service.save("WS-INTERNAL-0001", sample(), Collections.emptyList());
        assertEquals(3, storedTraces.size());
        assertEquals("Shipment update", legacy.getDescription());
        assertEquals("Departed from Origin location, Origin location", legacy.getDescriptionEn());
        assertEquals("Logistics facility", legacy.getTraceLocation());
        verify(parcels, times(1)).insert(any(Parcel.class));
        verify(traces, times(2)).insert(any(ParcelTrace.class));
        verify(traces, never()).deleteByMainId(anyString());
    }

    @Test
    void emptyInputAndMalformedResponsesDoNotWrite() throws Exception {
        ParcelMapper parcels = mock(ParcelMapper.class);
        ParcelTraceMapper traces = mock(ParcelTraceMapper.class);
        WSParcelService service = service(parcels, traces);
        WSTrackResponse.TrackData data = sample();
        assertThrows(IllegalArgumentException.class, () -> service.save("", data, null));
        data.getTrackDetails().get(0).setTrack_date("bad date");
        assertThrows(RuntimeException.class, () -> service.save(data.getTrackingNumber(), data, null));
        data.setTrackDetails(Collections.emptyList());
        assertFalse(service.save(data.getTrackingNumber(), data, null));
        verifyNoInteractions(parcels, traces);
    }

    @Test
    void lastThirtyDaysAreDefaultAndDateBoundsAreInclusiveAndValidated() throws Exception {
        assertEquals(java.time.LocalDate.now().minusDays(29), WSJob.Options.parse(null).startDate());
        assertEquals(java.time.LocalDate.now().plusDays(1), WSJob.Options.parse("{}").endExclusive());
        assertEquals(30, java.time.temporal.ChronoUnit.DAYS.between(WSJob.Options.parse("{}").startDate(), WSJob.Options.parse("{}").endExclusive()));
        WSJob.Options options = WSJob.Options.parse("{\"startDate\":\"2026-09-26\",\"endDate\":\"2026-09-26\"}");
        assertEquals("2026-09-27", options.endExclusive().toString());
        assertEquals("2026-08-13", WSJob.Options.parse("{\"endDate\":\"2026-09-11\"}").startDate().toString());
        assertEquals("2026-09-05", WSJob.Options.parse("{\"startDate\":\"2026-09-05\",\"endDate\":\"2026-09-11\"}").startDate().toString());
        assertThrows(Exception.class, () -> WSJob.Options.parse("{\"pageSize\":0}"));
        assertThrows(Exception.class, () -> WSJob.Options.parse("{\"endDate\":\"bad\"}"));
        assertThrows(Exception.class, () -> WSJob.Options.parse("{\"startDate\":\"2026-09-27\",\"endDate\":\"2026-09-26\"}"));
    }

    @Test
    void distinctUnknownEventsAtSameTimeKeepDifferentFingerprints() throws Exception {
        WSTrackResponse.TrackData data = sample();
        data.getTrackDetails().get(0).setTrack_content("深圳国际交换局已封发");
        data.getTrackDetails().get(1).setTrack_content("上海邮件处理中心已收寄");
        data.getTrackDetails().get(1).setTrack_date(data.getTrackDetails().get(0).getTrack_date());
        List<ParcelTrace> traces = WSParcelService.convert(data, normalizer);
        assertEquals("Shipment update", traces.get(0).getDescription());
        assertEquals(traces.get(0).getDescription(), traces.get(1).getDescription());
        assertNotEquals(traces.get(0).getTraceCode(), traces.get(1).getTraceCode());
        assertEquals(10, traces.get(0).getTraceCode().length());
    }

    @Test
    void mergesSimultaneousSameStatusEventsToRespectDatabaseUniqueIndexes() throws Exception {
        ParcelMapper parcels = mock(ParcelMapper.class);
        ParcelTraceMapper traces = mock(ParcelTraceMapper.class);
        WSParcelService service = service(parcels, traces);
        when(parcels.searchByBillCode(anyList())).thenReturn(Collections.emptyList());
        when(parcels.insert(any(Parcel.class))).thenAnswer(invocation -> {
            ((Parcel) invocation.getArgument(0)).setId("p1");
            return 1;
        });
        when(traces.selectByMainId("p1")).thenReturn(Collections.emptyList());
        List<ParcelTrace> inserted = new ArrayList<>();
        when(traces.insert(any(ParcelTrace.class))).thenAnswer(invocation -> {
            ParcelTrace trace = invocation.getArgument(0);
            trace.setId("trace-1");
            inserted.add(trace);
            return 1;
        });
        WSTrackResponse.TrackData data = sample();
        data.getTrackDetails().get(0).setTrack_content("Shipment received at facility");
        data.getTrackDetails().get(1).setTrack_content("Processing at facility");
        data.getTrackDetails().get(1).setTrack_date(data.getTrackDetails().get(0).getTrack_date());
        service.save("WS-INTERNAL-0001", data, null);
        assertEquals(1, inserted.size());
        assertEquals("Shipment received at facility | Processing at facility", inserted.get(0).getDescription());
        verify(traces, times(1)).updateById(any(ParcelTrace.class));
    }

    private static WSParcelService service(ParcelMapper parcels, ParcelTraceMapper traces) throws Exception {
        WSParcelService service = new WSParcelService();
        set(service, "parcelMapper", parcels);
        set(service, "traceMapper", traces);
        return service;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static WSTrackResponse.TrackData sample() throws Exception {
        return new ObjectMapper().readValue("{\"trackingNumber\":\"WS-TEST-0001\","
                + "\"referenceNumber\":\"REF-TEST-0001\",\"consigneeCountry\":\"FR\",\"product_id\":\"product\","
                + "\"track_status\":8,\"trackDate\":\"2026-09-26T03:03:24.000+00:00\","
                + "\"trackContent\":\"Your shipment has been delivered successfully\",\"unknownField\":true,"
                + "\"trackDetails\":[{\"track_content\":\"订单已创建\",\"track_city\":\"默认地点\","
                + "\"track_date\":\"2026-08-26T20:15:01.000+00:00\"},{"
                + "\"track_content\":\"Your shipment has been delivered successfully\","
                + "\"track_date\":\"2026-09-26T03:03:24.000+00:00\"}]}", WSTrackResponse.TrackData.class);
    }
}
