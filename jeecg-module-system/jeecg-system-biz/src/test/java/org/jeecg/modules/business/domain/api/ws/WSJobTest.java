package org.jeecg.modules.business.domain.api.ws;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.jeecg.modules.business.domain.job.WSJob;
import org.jeecg.modules.business.mapper.WSTrackOrderMapper;
import org.jeecg.modules.business.service.impl.WSParcelService;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class WSJobTest {
    @Test
    void continuesAfterFailureAndLogsTotalProgressAndCompletion() throws Exception {
        WSTrackOrderMapper mapper = mock(WSTrackOrderMapper.class);
        WSTrackClient client = mock(WSTrackClient.class);
        WSParcelService service = mock(WSParcelService.class);
        WSJob job = job(mapper, client, service);
        when(mapper.countBillCodes(any(LocalDate.class), any(LocalDate.class), anyList())).thenReturn(2L);
        when(mapper.findBillCodes(eq(""), eq(1), any(LocalDate.class), any(LocalDate.class), anyList())).thenReturn(Collections.singletonList("A"));
        when(mapper.findBillCodes(eq("A"), eq(1), any(LocalDate.class), any(LocalDate.class), anyList())).thenReturn(Collections.singletonList("B"));
        when(mapper.findBillCodes(eq("B"), eq(1), any(LocalDate.class), any(LocalDate.class), anyList())).thenReturn(Collections.emptyList());
        when(client.query("A")).thenThrow(new java.io.IOException("provider failed"));
        WSTrackResponse.TrackData data = new WSTrackResponse.TrackData();
        data.setTrackingNumber("B");
        when(client.query("B")).thenReturn(Collections.singletonList(data));
        when(service.save(eq("B"), same(data), anyList())).thenReturn(true);
        Logger logger = (Logger) LoggerFactory.getLogger(WSJob.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThrows(JobExecutionException.class, () -> job.execute(context()));
            assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("WSJob started:") && e.getFormattedMessage().contains("totalOrders=2")));
            assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("WSJob progress: processed=2/2") && e.getFormattedMessage().contains("saved=1, empty=0, failed=1")));
            assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("WSJob finished: totalOrders=2, queried=2, saved=1, empty=0, failed=1")));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        verify(service).save(eq("B"), same(data), anyList());
        verify(mapper).findBillCodes(eq("B"), eq(1), any(LocalDate.class), any(LocalDate.class), anyList());
    }

    @Test
    void zeroOrdersSkipsQueriesAndLogsZeroSummary() throws Exception {
        WSTrackOrderMapper mapper = mock(WSTrackOrderMapper.class);
        WSTrackClient client = mock(WSTrackClient.class);
        WSParcelService service = mock(WSParcelService.class);
        Logger logger = (Logger) LoggerFactory.getLogger(WSJob.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            job(mapper, client, service).execute(context());
            assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("WSJob finished: totalOrders=0, queried=0, saved=0, empty=0, failed=0")));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        verify(mapper, never()).findBillCodes(anyString(), anyInt(), any(), any(), anyList());
        verify(client, never()).query(anyString());
        verifyNoInteractions(service);
    }

    @Test
    void countAndPaginationShareCompanyAndDateFilters() throws Exception {
        String resource = "org/jeecg/modules/business/mapper/xml/WSTrackOrderMapper.xml";
        Configuration config = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("after", "A");
        params.put("limit", 200);
        params.put("startDate", LocalDate.parse("2026-09-01"));
        params.put("endExclusive", LocalDate.parse("2026-10-01"));
        params.put("transporters", Collections.singletonList("威狮测试路线"));
        String namespace = WSTrackOrderMapper.class.getName() + ".";
        String sql = config.getMappedStatement(namespace + "findBillCodes").getBoundSql(params).getSql().replaceAll("\\s+", " ");
        String countSql = config.getMappedStatement(namespace + "countBillCodes").getBoundSql(params).getSql().replaceAll("\\s+", " ");
        for (String query : Arrays.asList(sql, countSql)) {
            assertTrue(query.contains("WHERE EXISTS"));
            assertTrue(query.contains("company.id = lc.logistic_company_id"));
            assertTrue(query.contains("lc.zh_name = po.logistic_channel_name"));
            assertTrue(query.contains("company.name LIKE '%威狮%'"));
            assertTrue(query.contains("shipping_time < ?"));
            assertTrue(query.contains("logistic_channel_name IN"));
            assertFalse(query.contains("logistic_channel_name LIKE"));
            assertFalse(query.contains("parcel_trace"));
        }
        assertTrue(sql.contains("internal_tracking_number > ?"));
        assertTrue(sql.contains("LIMIT ?"));
        assertTrue(countSql.contains("COUNT(DISTINCT po.internal_tracking_number)"));
        assertFalse(countSql.contains("LIMIT"));
        assertFalse(countSql.contains("internal_tracking_number >"));
    }

    private static WSJob job(WSTrackOrderMapper mapper, WSTrackClient client, WSParcelService service) throws Exception {
        WSJob job = new WSJob();
        set(job, "orderMapper", mapper);
        set(job, "client", client);
        set(job, "parcelService", service);
        return job;
    }
    private static JobExecutionContext context() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap parameters = new JobDataMap();
        parameters.put("parameter", "{\"pageSize\":1,\"requestIntervalMs\":0}");
        when(context.getMergedJobDataMap()).thenReturn(parameters);
        return context;
    }
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
