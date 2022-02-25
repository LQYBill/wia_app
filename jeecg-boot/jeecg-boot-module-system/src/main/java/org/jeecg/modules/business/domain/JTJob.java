package org.jeecg.modules.business.domain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.jtapi.JTParcelTrace;
import org.jeecg.modules.business.domain.jtapi.JTRequest;
import org.jeecg.modules.business.domain.jtapi.JTResponse;
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
import java.util.List;

@Slf4j
public class JTJob implements Job {

    @Autowired
    private IParcelService parcelService;
    @Autowired
    private IPlatformOrderService platformOrderService;

    private static final Integer DEFAULT_NUMBER_OF_DAYS = 10;
    private static final String DEFAULT_TRANSPORTER = "法国专线普货";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(DEFAULT_NUMBER_OF_DAYS);
        String transporter = DEFAULT_TRANSPORTER;
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
                if (!jsonObject.isNull("transporter")) {
                    transporter = jsonObject.getString("transporter");
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

        log.info("Starting to retrieve parcel traces of {} from {} to {}", transporter, startDate, endDate);
        List<String> billCodes = platformOrderService.fetchBillCodesOfParcelsWithoutTrace(
                Date.valueOf(startDate), Date.valueOf(endDate), transporter);
        log.info("{} parcels without trace in total", billCodes.size());
        List<List<String>> billCodeLists = Lists.partition(billCodes, 50);
        log.info("Requests will be divided in to {} parts", billCodeLists.size());
        List<JTParcelTrace> parcelTraces = new ArrayList<>();
        try {
            for (List<String> billCodeList : billCodeLists) {
                JTRequest JTRequest = new JTRequest(billCodeList);
                HttpResponse response = JTRequest.send();
                HttpEntity entity = response.getEntity();
                // String of the response
                String responseString = EntityUtils.toString(entity, "UTF-8");
                JTResponse jtResponse = mapper.readValue(responseString, JTResponse.class);
                List<JTParcelTrace> tracesList = jtResponse.getResponseItems().get(0).getTracesList();
                parcelTraces.addAll(tracesList);
                log.info("{} parcels added to the queue to be inserted into DB.", tracesList.size());
            }
        } catch (IOException e) {
            log.error("Error while parsing response into String", e);
        }
        log.info("{} parcels have been retrieved.", parcelTraces.size());
        parcelService.saveParcelAndTraces(parcelTraces);
    }

}
