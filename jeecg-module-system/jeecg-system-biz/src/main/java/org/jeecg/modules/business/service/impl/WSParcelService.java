package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.domain.api.ws.WSTrackResponse.TrackData;
import org.jeecg.modules.business.domain.api.ws.WSTrackResponse.TrackDetail;
import org.jeecg.modules.business.domain.api.ws.WSTraceNormalizer;
import org.jeecg.modules.business.entity.Parcel;
import org.jeecg.modules.business.entity.ParcelTrace;
import org.jeecg.modules.business.mapper.ParcelMapper;
import org.jeecg.modules.business.mapper.ParcelTraceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;

@Service
public class WSParcelService {
    @Autowired
    private ParcelMapper parcelMapper;
    @Autowired
    private ParcelTraceMapper traceMapper;

    /** Per-parcel transaction: a malformed timestamp or write failure leaves existing data intact. */
    @Transactional(rollbackFor = Exception.class)
    public boolean save(String requestedBillCode, TrackData data, List<String> neutralKeywords) {
        if (data == null || !StringUtils.hasText(requestedBillCode)) {
            throw new IllegalArgumentException("Weishi response and requested bill code must not be empty");
        }
        WSTraceNormalizer normalizer = new WSTraceNormalizer(neutralKeywords);
        List<ParcelTrace> incoming = convert(data, normalizer);
        if (incoming.isEmpty()) return false;

        List<Parcel> parcels = parcelMapper.searchByBillCode(Collections.singletonList(requestedBillCode));
        if (parcels.size() > 1) throw new IllegalStateException("Duplicate parcel bill code");
        Parcel parcel = parcels.isEmpty() ? new Parcel() : parcels.get(0);
        Date now = new Date();
        parcel.setBillCode(requestedBillCode);
        // A single queried internal bill code can return a different last-mile tracking number.
        parcel.setThirdBillCode(StringUtils.hasText(data.getTrackingNumber()) ? data.getTrackingNumber() : requestedBillCode);
        if (StringUtils.hasText(data.getReferenceNumber())) parcel.setOrderNo(data.getReferenceNumber());
        if (StringUtils.hasText(data.getConsigneeCountry())) parcel.setCountry(data.getConsigneeCountry());
        if (StringUtils.hasText(data.getProduct_id())) parcel.setProductCode(data.getProduct_id());
        parcel.setUpdateBy("ws api");
        parcel.setUpdateTime(now);
        if (parcels.isEmpty()) {
            parcel.setCreateBy("ws api");
            parcel.setCreateTime(now);
            parcelMapper.insert(parcel);
        } else {
            parcelMapper.updateById(parcel);
        }

        List<ParcelTrace> existing = new ArrayList<>();
        for (ParcelTrace trace : traceMapper.selectByMainId(parcel.getId())) {
            // Also neutralise previously saved rows; retain history missing from a later API response.
            trace.setDescription(normalizer.description(trace.getDescription()));
            trace.setDescriptionEn(normalizer.description(StringUtils.hasText(trace.getDescriptionEn())
                    ? trace.getDescriptionEn() : trace.getDescription()));
            trace.setTraceLocation(normalizer.location(trace.getTraceLocation()));
            trace.setUpdateBy("ws api");
            trace.setUpdateTime(now);
            traceMapper.updateById(trace);
            existing.add(trace);
        }
        for (ParcelTrace trace : incoming) {
            ParcelTrace previous = findExisting(existing, trace);
            trace.setParcelId(parcel.getId());
            trace.setUpdateBy("ws api");
            trace.setUpdateTime(now);
            if (previous == null) {
                trace.setCreateBy("ws api");
                trace.setCreateTime(now);
                traceMapper.insert(trace);
                existing.add(trace);
            } else {
                previous.setDescription(mergeText(previous.getDescription(), trace.getDescription()));
                previous.setDescriptionEn(mergeText(previous.getDescriptionEn(), trace.getDescriptionEn()));
                previous.setTraceLocation(mergeText(previous.getTraceLocation(), trace.getTraceLocation()));
                if (trace.getTraceStatus() != null) previous.setTraceStatus(trace.getTraceStatus());
                previous.setUpdateBy("ws api");
                previous.setUpdateTime(now);
                traceMapper.updateById(previous);
            }
        }
        return true;
    }

    public static List<ParcelTrace> convert(TrackData data, WSTraceNormalizer normalizer) {
        List<TrackDetail> details = data.getTrackDetails();
        if (details == null || details.isEmpty()) return Collections.emptyList();
        List<ParcelTrace> traces = new ArrayList<>();
        for (TrackDetail detail : details) {
            if (detail == null || !StringUtils.hasText(detail.getTrack_content())) {
                throw new IllegalArgumentException("Weishi track detail has no content");
            }
            ParcelTrace trace = new ParcelTrace();
            trace.setScanTime(parseUtc(detail.getTrack_date()));
            Integer status = detail.getTrack_status();
            // A top-level delivered flag applies only to the corresponding latest event.
            if (status == null && StringUtils.hasText(data.getTrackDate())
                    && trace.getScanTime().equals(parseUtc(data.getTrackDate()))
                    && Objects.equals(detail.getTrack_content(), data.getTrackContent())) {
                status = data.getTrack_status();
            }
            trace.setDescription(normalizer.description(detail.getTrack_content()));
            trace.setDescriptionEn(trace.getDescription());
            trace.setTraceLocation(normalizer.location(detail.getTrack_city()));
            trace.setTraceStatus(status == null ? null : status.toString());
            // Missing carrier codes are common. Keep a stable fingerprint of the original event
            // so distinct unknown Chinese events remain distinct after neutralization.
            trace.setTraceCode(DigestUtils.md5DigestAsHex((trace.getScanTime().getTime() + "\n"
                    + detail.getTrack_content() + "\n" + Objects.toString(detail.getTrack_city(), "")
                    + "\n" + Objects.toString(detail.getTrack_status_code(), ""))
                    .getBytes(StandardCharsets.UTF_8)).substring(0, 10));
            trace.setScanType(normalizer.scanType(detail.getTrack_content(), status));
            traces.add(trace);
        }
        return traces;
    }

    public static Date parseUtc(String value) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException("Weishi track date is missing");
        try {
            return Date.from(OffsetDateTime.parse(value).toInstant());
        } catch (java.time.format.DateTimeParseException ex) {
            // The documentation also permits an offset-free UTC date.
            DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
                    .withResolverStyle(ResolverStyle.STRICT);
            return Date.from(LocalDateTime.parse(value, format).toInstant(ZoneOffset.UTC));
        }
    }

    private static ParcelTrace findExisting(List<ParcelTrace> existing, ParcelTrace incoming) {
        for (ParcelTrace trace : existing) {
            boolean sameCode = StringUtils.hasText(trace.getTraceCode()) && trace.getTraceCode().equals(incoming.getTraceCode());
            boolean sameTime = Objects.equals(trace.getScanTime(), incoming.getScanTime());
            boolean sameType = Objects.equals(trace.getScanType(), incoming.getScanType());
            boolean sameDescription = Objects.equals(trace.getDescription(), incoming.getDescription());
            // Match the actual database uniqueness constraints, retaining all distinct texts.
            if (sameCode || (sameTime && (sameType || sameDescription))) return trace;
        }
        return null;
    }

    private static String mergeText(String previous, String incoming) {
        LinkedHashSet<String> texts = new LinkedHashSet<>();
        if (StringUtils.hasText(previous)) texts.addAll(Arrays.asList(previous.split(" \\| ")));
        if (StringUtils.hasText(incoming)) texts.addAll(Arrays.asList(incoming.split(" \\| ")));
        if (texts.isEmpty()) return null;
        String merged = String.join(" | ", texts);
        merged = WSTraceNormalizer.fitDatabaseText(merged);
        return merged;
    }
}
