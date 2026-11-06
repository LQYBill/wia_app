package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.api.whop.WhopClient;
import org.jeecg.modules.business.domain.api.whop.WhopOrderConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WhopOrderPullService {
    private final WhopClient client;
    private final JdbcTemplate jdbc;

    public WhopOrderPullService(WhopClient client, JdbcTemplate jdbc) {
        this.client = client;
        this.jdbc = jdbc;
    }

    /** Failed runs replay their window; the unique key makes partial-page saves safe. */
    public int pull(String accountId, Instant initialStart) {
        if (accountId == null || !accountId.matches("biz_[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("A valid Whop accountId (biz_...) is required");
        }
        List<String> states = jdbc.queryForList(
                "select last_completed_at from whop_sync_state where account_id = ?", String.class, accountId);
        Instant from;
        if (states.isEmpty()) {
            if (initialStart == null) throw new IllegalArgumentException("First run requires startTime in ISO-8601 UTC");
            from = initialStart;
        } else {
            from = Instant.parse(states.get(0)).minus(5, ChronoUnit.MINUTES);
        }
        // Fixed window plus overlap captures late updates and boundary timestamps.
        Instant until = Instant.now().minusSeconds(30);
        if (!from.isBefore(until)) throw new IllegalArgumentException("startTime must precede the current sync window");
        String cursor = null;
        Set<String> cursors = new HashSet<>();
        int count = 0;
        do {
            JSONObject page = client.payments(accountId, from, until, cursor);
            JSONArray data = page.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                save(accountId, client.payment(data.getJSONObject(i).getString("id")));
                count++;
            }
            JSONObject info = page.getJSONObject("page_info");
            if (!info.containsKey("has_next_page")) throw new IllegalStateException("Missing Whop pagination flag");
            if (!info.getBooleanValue("has_next_page")) break;
            cursor = info.getString("end_cursor");
            if (cursor == null || cursor.isBlank() || !cursors.add(cursor)) {
                throw new IllegalStateException("Invalid or repeated Whop cursor");
            }
        } while (true);
        // Current API lists by creation time. Rotate through older payments to capture
        // late payment success, refunds and cancellations outside the creation window.
        List<String> older = jdbc.queryForList("select payment_id from whop_order where account_id = ? "
                + "and source_created_at < ? order by updated_at, payment_id limit 100", String.class, accountId, from.toString());
        for (String id : older) save(accountId, client.payment(id));
        // Do not advance the checkpoint until every page has been persisted.
        jdbc.update("insert into whop_sync_state (account_id, last_completed_at) values (?, ?) "
                + "on duplicate key update last_completed_at = values(last_completed_at)", accountId, until.toString());
        return count;
    }

    void save(String accountId, JSONObject payment) {
        if (payment == null || payment.getString("id") == null || payment.getString("id").isBlank()) {
            throw new IllegalStateException("Whop payment has no id");
        }
        if (!accountId.equals(payment.getString("account_id"))) {
            throw new IllegalStateException("Whop payment belongs to a different account");
        }
        // Preserve all statuses: refunds and cancellations must also be recorded.
        // Re-fetching never resets push state or sends a duplicate order to Mabang.
        jdbc.update("insert into whop_order (account_id, payment_id, payment_status, payment_substatus, sku, "
                        + "currency, total, source_created_at, source_updated_at, raw_json, created_at, updated_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)) "
                        + "on duplicate key update payment_status=values(payment_status), "
                        + "payment_substatus=values(payment_substatus), sku=values(sku), currency=values(currency), "
                        + "total=values(total), source_created_at=values(source_created_at), "
                        + "source_updated_at=values(source_updated_at), raw_json=values(raw_json), "
                        + "push_status=case when push_status='SENT' and "
                        + "(values(payment_status)<>'paid' or values(payment_substatus)<>'succeeded') "
                        + "then 'REVIEW_REQUIRED' else push_status end, updated_at=UTC_TIMESTAMP(3)",
                accountId, payment.getString("id"), payment.getString("status"), payment.getString("substatus"),
                payment.getString("sku"), payment.getString("currency"), WhopOrderConverter.money(payment, "total"),
                payment.getString("created_at"), payment.getString("updated_at"), payment.toJSONString());
    }
}
