package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.whop.WhopClient;
import org.jeecg.modules.business.domain.api.whop.WhopShipmentCandidate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WhopShipmentSyncService {
    private static final Pattern MULTIPLE_NUMBERS = Pattern.compile("[\\s,;，；]");
    private final WhopShipmentSyncRepository repository;
    private final WhopClient client;

    public WhopShipmentSyncService(WhopShipmentSyncRepository repository, WhopClient client) {
        this.repository = repository;
        this.client = client;
    }

    public int sync(String account, String shop, int batchSize, boolean dryRun) {
        if (account == null || !account.matches("biz_[A-Za-z0-9]+")) throw new IllegalArgumentException("Invalid Whop accountId");
        if (shop == null || shop.isBlank()) throw new IllegalArgumentException("Missing Mabang shopName");
        if (batchSize < 1 || batchSize > 500) throw new IllegalArgumentException("batchSize must be between 1 and 500");
        List<WhopShipmentCandidate> candidates = repository.candidates(account, shop, batchSize);
        if (dryRun) {
            log.info("Whop tracking dry run: {} candidate payments", candidates.size());
            return 0;
        }
        int synced = 0;
        for (WhopShipmentCandidate candidate : candidates) {
            String payment = candidate.getPaymentId();
            String number = candidate.getTrackingNumber();
            String key = idempotencyKey(account, payment, number);
            // Durable request and deterministic key survive a timeout or process restart.
            repository.begin(account, candidate, key);
            try {
                if (candidate.getTrackingCount() != 1 || number == null || number.isBlank()
                        || number.length() > 255 || MULTIPLE_NUMBERS.matcher(number).find()) {
                    repository.failed(account, payment, "CONFLICT", "Multiple or invalid carrier tracking numbers", null);
                    continue;
                }
                JSONObject shipment = client.shipmentForPayment(payment);
                if (shipment == null) {
                    try {
                        shipment = client.createShipment(account, payment, number, key);
                    } catch (HttpClientErrorException.Conflict e) {
                        // Another request may have completed; only a verified GET counts as success.
                        shipment = client.shipmentForPayment(payment);
                        if (shipment == null) throw e;
                    }
                }
                validateShipment(account, payment, shipment);
                if (!number.equals(shipment.getString("tracking_number"))) {
                    repository.failed(account, payment, "CONFLICT", "Whop already has a different tracking number", shipment);
                    continue;
                }
                repository.synced(account, payment, shipment);
                synced++;
            } catch (Exception e) {
                String reason = e instanceof RestClientResponseException
                        ? "Whop HTTP " + ((RestClientResponseException) e).getRawStatusCode()
                        : e.getClass().getSimpleName();
                // Avoid persisting tokens, customer details or raw HTTP error bodies.
                repository.failed(account, payment, "FAILED", reason, null);
                log.warn("Whop tracking upload failed for {} ({})", payment, reason);
                if (e instanceof RestClientResponseException) {
                    int code = ((RestClientResponseException) e).getRawStatusCode();
                    if (code == 401 || code == 403 || code == 429) throw (RestClientResponseException) e;
                }
            }
        }
        log.info("Whop tracking sync: {} candidates, {} confirmed", candidates.size(), synced);
        return synced;
    }

    private static void validateShipment(String account, String payment, JSONObject shipment) {
        if (shipment == null || shipment.getString("id") == null || !shipment.getString("id").startsWith("ship_")
                || !account.equals(shipment.getString("account_id")) || !payment.equals(shipment.getString("payment_id"))) {
            throw new IllegalStateException("Whop shipment identity mismatch");
        }
    }

    static String idempotencyKey(String account, String payment, String number) {
        return "wia-whop-shipment-" + DigestUtils.md5DigestAsHex(
                (account + "\n" + payment + "\n" + number).getBytes(StandardCharsets.UTF_8));
    }
}
