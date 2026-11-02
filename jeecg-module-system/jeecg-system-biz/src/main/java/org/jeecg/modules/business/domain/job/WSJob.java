package org.jeecg.modules.business.domain.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.ws.WSTrackClient;
import org.jeecg.modules.business.domain.api.ws.WSTrackResponse.TrackData;
import org.jeecg.modules.business.mapper.WSTrackOrderMapper;
import org.jeecg.modules.business.service.impl.WSParcelService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Weishi route tracking sync, configured by the same Quartz administration as YDJob. */
@Slf4j
@DisallowConcurrentExecution
public class WSJob implements Job {
    private static final int PROGRESS_LOG_INTERVAL = 25;
    @Autowired
    private WSTrackOrderMapper orderMapper;
    @Autowired
    private WSTrackClient client;
    @Autowired
    private WSParcelService parcelService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final Options options;
        try {
            options = Options.parse(context.getMergedJobDataMap().getString("parameter"));
            client.validateConfiguration();
        } catch (Exception ex) {
            throw new JobExecutionException("Invalid WSJob configuration", ex);
        }
        long started = System.nanoTime();
        long totalOrders = orderMapper.countBillCodes(options.startDate(), options.endExclusive(), options.transporters);
        int queried = 0, saved = 0, empty = 0, failed = 0, pageNumber = 0;
        String after = "";
        log.info("WSJob started: startDate={}, endDate={}, totalOrders={}, pageSize={}",
                options.startDate(), options.endExclusive().minusDays(1), totalOrders, options.pageSize);
        try {
            if (totalOrders == 0) return;
            List<String> page;
            do {
                page = orderMapper.findBillCodes(after, options.pageSize, options.startDate(),
                        options.endExclusive(), options.transporters);
                if (!page.isEmpty()) {
                    pageNumber++;
                    log.info("WSJob page: page={}, pageOrders={}, processed={}, totalOrders={}",
                            pageNumber, page.size(), queried, totalOrders);
                }
                for (String billCode : page) {
                    if (Thread.currentThread().isInterrupted()) throw new JobExecutionException("WSJob interrupted");
                    queried++;
                    try {
                        List<TrackData> data = client.query(billCode);
                        if (data.isEmpty()) empty++;
                        else {
                            // Single-number requests avoid ambiguous batch/result association.
                            if (data.size() != 1) throw new IllegalStateException("Unexpected number of Weishi results");
                            if (parcelService.save(billCode, data.get(0), options.neutralKeywords)) saved++;
                            else empty++;
                        }
                    } catch (Exception ex) {
                        failed++;
                        log.warn("WSJob failed for billCode={}, errorType={}", billCode, ex.getClass().getSimpleName());
                    }
                    if (queried % PROGRESS_LOG_INTERVAL == 0 || queried == totalOrders) {
                        log.info("WSJob progress: processed={}/{}, remaining={}, saved={}, empty={}, failed={}, elapsedMs={}",
                                queried, totalOrders, Math.max(0, totalOrders - queried), saved, empty, failed,
                                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                    }
                    try {
                        Thread.sleep(options.requestIntervalMs);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new JobExecutionException("WSJob interrupted", ex);
                    }
                }
                if (!page.isEmpty()) after = page.get(page.size() - 1);
            } while (page.size() == options.pageSize);
        } finally {
            log.info("WSJob finished: totalOrders={}, queried={}, saved={}, empty={}, failed={}, elapsedMs={}",
                    totalOrders, queried, saved, empty, failed, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        }
        if (failed > 0) throw new JobExecutionException("WSJob completed with " + failed + " failed parcels; see logs");
    }

    @Data
    public static class Options {
        private String startDate;
        private String endDate;
        private int pageSize = 200;
        private long requestIntervalMs = 200;
        private List<String> transporters = Collections.emptyList();
        private List<String> neutralKeywords = Collections.emptyList();

        public static Options parse(String parameter) throws Exception {
            Options options = parameter == null || parameter.trim().isEmpty()
                    ? new Options() : new ObjectMapper().readValue(parameter, Options.class);
            if (options == null || options.pageSize < 1 || options.pageSize > 1000
                    || options.requestIntervalMs < 0 || options.requestIntervalMs > 60000) {
                throw new IllegalArgumentException("Invalid WSJob pageSize or requestIntervalMs");
            }
            // Include today: 30 calendar dates, with an exclusive bound at tomorrow midnight.
            if (options.endDate == null || options.endDate.trim().isEmpty()) options.endDate = LocalDate.now().toString();
            if (options.startDate == null || options.startDate.trim().isEmpty()) {
                options.startDate = LocalDate.parse(options.endDate).minusDays(29).toString();
            }
            if (!options.startDate().isBefore(options.endExclusive())) {
                throw new IllegalArgumentException("startDate must not be after endDate");
            }
            return options;
        }
        public LocalDate startDate() { return startDate == null ? null : LocalDate.parse(startDate); }
        public LocalDate endExclusive() { return endDate == null ? null : LocalDate.parse(endDate).plusDays(1); }
    }
}
