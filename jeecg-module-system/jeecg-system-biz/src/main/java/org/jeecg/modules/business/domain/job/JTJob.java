package org.jeecg.modules.business.domain.job;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.api.jt.JTParcelTrace;
import org.jeecg.modules.business.domain.api.jt.JTRequest;
import org.jeecg.modules.business.domain.api.jt.JTResponse;
import org.jeecg.modules.business.service.IParcelService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class JTJob implements Job {

    @Autowired
    private IParcelService parcelService;
    @Autowired
    private IPlatformOrderService platformOrderService;

    private static final Integer DEFAULT_NUMBER_OF_DAYS = 15;

    private static final Integer DEFAULT_NUMBER_OF_THREADS = 10;

    private static final Integer DEFAULT_MAXIMUM_NUMBER_OF_PARCELS_PER_TRANSACTION = 800;
    private static final List<String> DEFAULT_TRANSPORTERS = Arrays.asList("法国专线普货", "珠海E邮宝-比利时", "珠海E邮宝-法国");

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(DEFAULT_NUMBER_OF_DAYS);
        List<String> transporters = DEFAULT_TRANSPORTERS;
        boolean overrideRestriction = false;
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String parameter = ((String) jobDataMap.get("parameter"));
        if (parameter != null) {
            try {
                JSONObject jsonObject = new JSONObject(parameter);
                if (!jsonObject.isNull("startDate")) {
                    String startDateStr = jsonObject.getString("startDate");
                    startDate = LocalDate.parse(startDateStr);
                }
                if (!jsonObject.isNull("endDate")) {
                    String endDateStr = jsonObject.getString("endDate");
                    endDate = LocalDate.parse(endDateStr);
                }
                if (!jsonObject.isNull("transporters")) {
                    JSONArray transporterArray = jsonObject.getJSONArray("transporters");
                    List<String> transporterList = new ArrayList<>();
                    for (int i = 0; i < transporterArray.length(); i++) {
                        transporterList.add(transporterArray.getString(i));
                    }
                    transporters = transporterList;
                }
                if (!jsonObject.isNull("override")) {
                    overrideRestriction = jsonObject.getBoolean("override");
                }
            } catch (JSONException e) {
                log.error("Error while parsing parameter as JSON, falling back to default parameters.");
            }
        }

        if (!endDate.isAfter(startDate)) {
            throw new RuntimeException("EndDate must be strictly greater than StartDate !");
        } else if (endDate.minusDays(30).isAfter(startDate) && !overrideRestriction) {
            throw new RuntimeException("No more than 30 days can separate startDate and endDate !");
        }

        log.info("Starting to retrieve parcel traces of {} from {} to {}", transporters, startDate, endDate);
        List<String> billCodes = platformOrderService.fetchBillCodesOfParcelsWithoutTrace(
                Date.valueOf(startDate), Date.valueOf(endDate), transporters);
        log.info("{} parcels without trace in total", billCodes.size());
        List<List<String>> billCodeLists = Lists.partition(billCodes, 10);
        log.info("Requests will be divided in to {} parts", billCodeLists.size());
        List<JTParcelTrace> parcelTraces = new ArrayList<>();
        List<JTRequest> jtRequests = new ArrayList<>();
        billCodeLists.forEach(billcodeList -> {
            JTRequest jtRequest = new JTRequest(billcodeList);
            jtRequests.add(jtRequest);
        });

        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_NUMBER_OF_THREADS);
        List<CompletableFuture<Boolean>> futures = jtRequests.stream()
                .map(request -> CompletableFuture.supplyAsync(() -> {
                    boolean success = false;
                    HttpEntity entity = request.send().getEntity();
                    try {
                        // String of the response
                        String responseString = EntityUtils.toString(entity, "UTF-8");
                        JTResponse jtResponse = mapper.readValue(responseString, JTResponse.class);
                        parcelTraces.addAll(jtResponse.getResponseItems().get(0).getTracesList());
                        success = true;
                    } catch (IOException e) {
                        log.error("Error while parsing response into String", e);
                    }
                    log.info("{} parcel added to the queue to be inserted into DB.", parcelTraces.size());
                    return success;
                }, executor))
                .collect(Collectors.toList());
        List<Boolean> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        long nbSuccesses = results.stream().filter(b -> b).count();
        log.info("{}/{} lots of 50 parcel traces have been retrieved.", nbSuccesses, jtRequests.size());

        List<List<JTParcelTrace>> parcelTraceList = Lists.partition(parcelTraces, DEFAULT_MAXIMUM_NUMBER_OF_PARCELS_PER_TRANSACTION);
        for (List<JTParcelTrace> parcelTracesPerTransaction : parcelTraceList) {
            parcelService.saveJTParcelAndTraces(parcelTracesPerTransaction);
        }
    }

}
