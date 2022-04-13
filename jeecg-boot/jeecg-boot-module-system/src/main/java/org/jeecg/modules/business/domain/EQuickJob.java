package org.jeecg.modules.business.domain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.equickapi.EQuickRequest;
import org.jeecg.modules.business.domain.equickapi.EQuickResponse;
import org.jeecg.modules.business.domain.equickapi.EQuickTraceData;
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
import java.util.Arrays;
import java.util.List;

@Slf4j
public class EQuickJob implements Job {

    @Autowired
    private IParcelService parcelService;
    @Autowired
    private IPlatformOrderService platformOrderService;

    private static final Integer DEFAULT_NUMBER_OF_DAYS = 10;
    private static final List<String> DEFAULT_TRANSPORTERS = Arrays.asList("EQ快速专线小包（D）", "EQ快速专线小包（带电）", "EQ美国快速专线小包",
            "Equick快速专线小包（AT/CZ/SK/HU/BG/GR/RO/SI）", "Equick欧美速递专线", "Equick法国快速专线小包");

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
        List<String> equickNumbers = platformOrderService.fetchBillCodesOfParcelsWithoutTrace(
                Date.valueOf(startDate), Date.valueOf(endDate), transporters);
        log.info("{} parcels without trace in total", equickNumbers.size());
        List<EQuickResponse> parcels = new ArrayList<>();
        try {
            for (String equickNumber : equickNumbers) {
                EQuickRequest equickRequest = new EQuickRequest(equickNumber);
                String responseString = equickRequest.send().getBody();
                EQuickResponse eQuickResponse = mapper.readValue(responseString, EQuickResponse.class);
                parcels.add(eQuickResponse);
                log.info("{} parcel added to the queue to be inserted into DB.", parcels.size());
            }
        } catch (IOException e) {
            log.error("Error while parsing response into String", e);
        }
        log.info("{} parcel traces have been retrieved.", parcels.size());
        parcelService.saveEQParcelAndTraces(parcels);
    }

}
