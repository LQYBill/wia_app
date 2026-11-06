package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.domain.api.whop.WhopClient;
import org.jeecg.modules.business.domain.api.whop.WhopMabangGateway;
import org.jeecg.modules.business.domain.api.whop.WhopOrderConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WhopOrderPushService {
    private final JdbcTemplate jdbc;
    private final WhopClient client;
    private final WhopOrderConverter converter;
    private final WhopMabangGateway mabang;
    private final WhopOrderPullService pull;

    public WhopOrderPushService(JdbcTemplate jdbc, WhopClient client, WhopOrderConverter converter,
                                WhopMabangGateway mabang, WhopOrderPullService pull) {
        this.jdbc = jdbc;
        this.client = client;
        this.converter = converter;
        this.mabang = mabang;
        this.pull = pull;
    }

    public int push(String accountId) {
        List<Map<String, Object>> orders = jdbc.queryForList("select payment_id, push_status from whop_order "
                + "where account_id = ? and (push_status='SUBMITTED' or "
                + "(push_status in ('PENDING','BLOCKED') and payment_status='paid' and payment_substatus='succeeded')) "
                + "order by updated_at, payment_id limit 100", accountId);
        int submitted = 0;
        for (Map<String, Object> row : orders) {
            String id = (String) row.get("payment_id");
            try {
                // Even an interrupted create may already exist remotely.
                JSONObject check = mabang.check(id);
                Integer isok = creationResult(check);
                if (Integer.valueOf(1).equals(isok)) {
                    finish(accountId, id, "SENT", check, null);
                    continue;
                }
                if ("SUBMITTED".equals(row.get("push_status"))) {
                    finish(accountId, id, "SUBMITTED", check, "Awaiting Mabang confirmation; do not recreate automatically");
                    continue;
                }
                if (!"200".equals(check.getString("code")) || !Integer.valueOf(0).equals(isok)) {
                    throw new IllegalStateException("Mabang preflight result unavailable");
                }
                JSONObject payment = client.payment(id);
                pull.save(accountId, payment);
                JSONObject request;
                try {
                    request = converter.convert(payment);
                } catch (IllegalArgumentException e) {
                    jdbc.update("update whop_order set push_status='BLOCKED', last_error=?, updated_at=UTC_TIMESTAMP(3) "
                            + "where account_id=? and payment_id=? and push_status in ('PENDING','BLOCKED')",
                            e.getMessage(), accountId, id);
                    continue;
                }
                // Persist the outbound request before the network call. Atomic claim also
                // protects against accidentally running a second job for this account.
                int claimed = jdbc.update("update whop_order set push_status='SUBMITTED', mabang_request_json=?, "
                                + "push_attempts=push_attempts+1, last_error=null, updated_at=UTC_TIMESTAMP(3) "
                                + "where account_id=? and payment_id=? and push_status in ('PENDING','BLOCKED')",
                        request.toJSONString(), accountId, id);
                if (claimed == 0) continue;
                JSONObject response = mabang.create(request);
                if ("200".equals(response.getString("code"))) {
                    // Accepted is not final success. Check on the next execution.
                    finish(accountId, id, "SUBMITTED", response, null);
                    submitted++;
                } else {
                    finish(accountId, id, "REJECTED", response, "Mabang rejected creation; inspect saved response");
                }
            } catch (Exception e) {
                log.warn("Whop push/check failed for {} ({})", id, e.getClass().getSimpleName());
                jdbc.update("update whop_order set last_error=?, updated_at=UTC_TIMESTAMP(3) where account_id=? and payment_id=?",
                        "Push/check failed: " + e.getClass().getSimpleName(), accountId, id);
            }
        }
        return submitted;
    }

    static Integer creationResult(JSONObject response) {
        if (response == null || !"200".equals(response.getString("code"))) return null;
        JSONObject data = response.getJSONObject("data");
        return data == null ? response.getInteger("isok") : data.getInteger("isok");
    }

    private void finish(String account, String id, String status, JSONObject response, String error) {
        jdbc.update("update whop_order set push_status=?, mabang_response_json=?, last_error=?, updated_at=UTC_TIMESTAMP(3) "
                + "where account_id=? and payment_id=?", status, response.toJSONString(), error, account, id);
    }
}
