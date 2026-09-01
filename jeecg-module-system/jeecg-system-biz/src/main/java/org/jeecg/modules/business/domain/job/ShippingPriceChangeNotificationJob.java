package org.jeecg.modules.business.domain.job;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.LogisticChannelPrice;
import org.jeecg.modules.business.mapper.ShippingPriceChangeMapper;
import org.jeecg.modules.business.service.EmailService;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.entity.ShippingPriceChangeSales;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ShippingPriceChangeNotificationJob implements Job {
    private static final String TEMPLATE = "shippingPriceChangeNotification.ftl";
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.10");
    @Autowired
    private IClientService clientService;
    @Autowired
    private ShippingPriceChangeMapper mapper;
    @Autowired
    private EmailService emailService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Parameters p = parse(context.getMergedJobDataMap());
        List<Client> clients = p.clientThresholds.isEmpty() ? clientService.list().stream()
                .filter(c -> "1".equals(c.getActive())).collect(Collectors.toList()) : clientService.getClientsByCodes(new ArrayList<>(p.clientThresholds.keySet()));
        if (clients.isEmpty()) return;
        Date start = Date.from(LocalDate.now().minusMonths(6).atStartOfDay(ZoneId.systemDefault()).toInstant());
        for (Client client : clients) {
            List<ShippingPriceChangeSales> sales = mapper.findSales(client.getId(), start);
            Map<String, BigDecimal> weights = weights(sales, p.effectiveDate);
            BigDecimal threshold = p.clientThresholds.getOrDefault(client.getInternalCode(), DEFAULT_THRESHOLD);
            List<ReportRow> rows = buildRows(sales, weights, p.effectiveDate, threshold);
            log.info("Built {} rows of report for client {}", rows.size(), client.getInternalCode());
            if (rows.isEmpty() || StringUtils.isBlank(client.getEmail())) continue;
            Map<String, Object> model = new HashMap<>();
            model.put("effectiveDate", fmt(p.effectiveDate));
            model.put("rows", rows);
            try {
                emailService.newSendSimpleMessage(client.getEmail(), "Évolution tarifaire - " + fmt(p.effectiveDate), TEMPLATE, model);
                log.info("Successfully send report to {}", client.getEmail());
            } catch (Exception e) {
                log.error("Failed to send report to {}", client.getEmail(), e);
            }
        }
    }

    private List<ReportRow> buildRows(List<ShippingPriceChangeSales> sales, Map<String, BigDecimal> weights, Date date, BigDecimal threshold) {
        Map<String, List<ShippingPriceChangeSales>> countries = sales.stream().collect(Collectors.groupingBy(ShippingPriceChangeSales::getCountry));
        List<ReportRow> result = new ArrayList<>();
        for (Map.Entry<String, List<ShippingPriceChangeSales>> countrySales : countries.entrySet()) {
            log.info("Going through sales for {}", countrySales.getKey());
            if (countrySales.getValue().isEmpty()) {
                log.info("No sales in this country, skipping");
                continue;
            }
            Map<String, List<ShippingPriceChangeSales>> skus = countrySales.getValue().stream().collect(Collectors.groupingBy(ShippingPriceChangeSales::getSkuCode));
            BigDecimal countryTotal = countrySales.getValue().stream().map(s -> nvl(s.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("Total sales for country {}", countryTotal);
            for (Map.Entry<String, List<ShippingPriceChangeSales>> skuSales : skus.entrySet()) {
                BigDecimal skuTotal = skuSales.getValue().stream().map(s -> nvl(s.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
                log.info("Total sales of SKU {} for country {}", skuSales.getKey(), skuTotal);
                if (countryTotal.signum() == 0 || skuTotal.divide(countryTotal, 8, RoundingMode.HALF_UP).compareTo(threshold) <= 0) {
                    log.info("Total sales of SKU {} not meeting {} threshold, skipping", skuSales.getKey(), threshold);
                    continue;
                }
                ShippingPriceChangeSales s = skuSales.getValue().stream().max(Comparator.comparing(x -> nvl(x.getQuantity()))).orElse(null);
                BigDecimal weight = s == null ? null : weights.get(s.getSkuId());
                if (s == null || weight == null || weight.equals(BigDecimal.ZERO) || StringUtils.isBlank(s.getChannelName())) continue;
                List<LogisticChannelPrice> history = mapper.findPriceHistory(s.getChannelName(), s.getCountry(), weight);
                LogisticChannelPrice oldP = history.stream().filter(x -> x.getEffectiveDate().before(date)).max(Comparator.comparing(LogisticChannelPrice::getEffectiveDate)).orElse(null);
                LogisticChannelPrice newP = history.stream().filter(x -> x.getEffectiveDate().after(date)).min(Comparator.comparing(LogisticChannelPrice::getEffectiveDate)).orElse(null);
                if (oldP != null && newP != null) {
                    log.info("Found latest price of {} before {}", oldP.getEffectiveDate(), date);
                    log.info("Found earliest price of {} after {}", newP.getEffectiveDate(), date);
                    result.add(ReportRow.of(s, weight, oldP, newP));
                }
            }
        }
        return result;
    }

    private Map<String, BigDecimal> weights(List<ShippingPriceChangeSales> sales, Date date) {
        List<String> ids = sales.stream().map(ShippingPriceChangeSales::getSkuId).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyMap();
        return mapper.findLatestWeights(ids, date).stream().collect(Collectors.toMap(m -> String.valueOf(m.get("skuId")), m -> new BigDecimal(String.valueOf(m.get("weight"))), (a, b) -> b));
    }

    private Parameters parse(JobDataMap data) {
        Parameters p = new Parameters();
        p.effectiveDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        String raw = (String) data.get("parameter");
        if (StringUtils.isBlank(raw)) return p;
        try {
            JSONObject json = new JSONObject(raw);
            if (!json.isNull("effectiveDate"))
                p.effectiveDate = new SimpleDateFormat("yyyy-MM-dd").parse(json.getString("effectiveDate"));
            if (!json.isNull("clients")) {
                JSONArray clients = json.getJSONArray("clients");
                for (int i = 0; i < clients.length(); i++) {
                    JSONObject client = clients.getJSONObject(i);
                    String code = client.getString("code").trim();
                    BigDecimal threshold = client.isNull("threshold") ? DEFAULT_THRESHOLD
                            : new BigDecimal(String.valueOf(client.get("threshold"))).movePointLeft(2);
                    if (StringUtils.isNotBlank(code) && threshold.signum() >= 0) {
                        p.clientThresholds.put(code, threshold);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid job parameter: " + raw, e);
        }
        return p;
    }

    private static BigDecimal nvl(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }

    private static String fmt(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    @Data
    private static class Parameters {
        private Map<String, BigDecimal> clientThresholds = new LinkedHashMap<>();
        private Date effectiveDate;
    }

    @Data
    public static class ReportRow {
        private String skuCode, skuName, imageUrl, countryWithFlag, oldDate, newDate;
        private BigDecimal oldTotal, newTotal, difference, changePercentage;

        static ReportRow of(ShippingPriceChangeSales s, BigDecimal weight, LogisticChannelPrice oldP, LogisticChannelPrice newP) {
            ReportRow r = new ReportRow();
            r.skuCode = s.getSkuCode();
            r.skuName = s.getSkuName();
            r.imageUrl = s.getImageUrl();
            String flag = s.getCountryCode().chars()
                    .mapToObj(c -> String.valueOf(Character.toChars(0x1F1E6 + c - 'A')))
                    .collect(Collectors.joining());
            r.countryWithFlag = s.getCountry() + " " + flag;
            r.oldDate = fmt(oldP.getEffectiveDate());
            r.newDate = fmt(newP.getEffectiveDate());
            r.oldTotal = nvl(oldP.getRegistrationFee()).add(oldP.calculateShippingPrice(weight)).setScale(2, RoundingMode.UP);
            r.newTotal = nvl(newP.getRegistrationFee()).add(newP.calculateShippingPrice(weight)).setScale(2, RoundingMode.UP);
            r.difference = r.newTotal.subtract(r.oldTotal);
            r.changePercentage = r.oldTotal.signum() == 0 ? null : r.difference.divide(r.oldTotal, 4, RoundingMode.UP).multiply(new BigDecimal("100"));
            return r;
        }
    }
}
