package org.jeecg.modules.business.domain.job;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.api.mabang.doSearchSkuList.*;
import org.jeecg.modules.business.domain.api.mabang.doSearchSkuList.SkuData;
import org.jeecg.modules.business.service.ISkuListMabangService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jeecg.modules.business.domain.api.mabang.doSearchSkuList.SkuStatus.*;

/**
 * A Job that retrieves all Sku from Mabang
 * if the sku is of status 3 (normal) and not in DB, then we insert it in DB
 */
@Slf4j
public class MabangSkuJob implements Job {

    @Autowired
    @Setter
    private ISkuListMabangService skuListMabangService;
    private static final Integer DEFAULT_NUMBER_OF_DAYS = 5;
    private static final DateType DEFAULT_DATE_TYPE = DateType.UPDATE;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDateTime endDateTime = LocalDateTime.now(ZoneId.of(ZoneId.SHORT_IDS.get("CTT")));
        LocalDateTime startDateTime = endDateTime.minusDays(DEFAULT_NUMBER_OF_DAYS);
        DateType dateType = DEFAULT_DATE_TYPE;
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String parameter = ((String) jobDataMap.get("parameter"));
        if (parameter != null) {
            try {
                JSONObject jsonObject = new JSONObject(parameter);
                if (!jsonObject.isNull("startDateTime")) {
                    String startDateStr = jsonObject.getString("startDateTime");
                    startDateTime = LocalDateTime.parse(startDateStr);
                }
                if (!jsonObject.isNull("endDateTime")) {
                    String endDateStr = jsonObject.getString("endDateTime");
                    endDateTime = LocalDateTime.parse(endDateStr);
                }
                if (!jsonObject.isNull("dateType")) {
                    dateType = DateType.fromCode(jsonObject.getInt("dateType"));
                }
            } catch (JSONException e) {
                log.error("Error while parsing parameter as JSON, falling back to default parameters.");
            }
        }

        if (!endDateTime.isAfter(startDateTime)) {
            throw new RuntimeException("EndDateTime must be strictly greater than StartDateTime !");
        } else if (endDateTime.minusDays(30).isAfter(startDateTime)) {
            throw new RuntimeException("No more than 30 days can separate startDateTime and endDateTime !");
        }

        try {
            while (startDateTime.until(endDateTime, ChronoUnit.HOURS) > 0) {
                LocalDateTime dayBeforeEndDateTime = endDateTime.minusDays(1);
                SkuListRequestBody body = SkuListRequestBodys.allSkuOfDateType(dayBeforeEndDateTime, endDateTime, dateType);
                SkuListRawStream rawStream = new SkuListRawStream(body);
                SkuListStream stream = new SkuListStream(rawStream);
                List<SkuData> skusFromMabang = stream.all();
                log.info("{} skus from {} to {} ({})to be inserted/updated.", skusFromMabang.size(),
                        dayBeforeEndDateTime, endDateTime, dateType);

                /**
                 *  DEBUG
                 */
                int cpt = 0;
                System.out.println("## DEBUG SKU MABANG JOB /!\\");
                for(SkuData sku : skusFromMabang ) {
                    if(!sku.getStatus().equals(Normal)) {
                        System.out.println("########### NOT SUPPOSED TO BE HERE ###########");
                    }
                    System.out.println("##" + cpt + "##\n" +sku.toString());
                    cpt++;
                }
                //TODO : filter status 3
                //skuListMabangService.saveSkuFromMabang(skusFromMabang);
                endDateTime = dayBeforeEndDateTime;
            }
        } catch (SkuListRequestErrorException e) {
            throw new RuntimeException(e);
        }
    }

}
