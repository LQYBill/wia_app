package org.jeecg.modules.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.api.whop.WhopShipmentCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WhopShipmentSyncRepository {
    private final JdbcTemplate jdbc;

    public WhopShipmentSyncRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<WhopShipmentCandidate> candidates(String accountId, String shopName, int limit) {
        // External carrier number from the existing Mabang order import, not the internal waybill.
        // Group before filtering successful uploads so split orders with distinct numbers cannot
        // silently send one arbitrarily selected parcel to a single-shipment payment.
        return jdbc.query("select w.payment_id, max(trim(po.tracking_number)) as tracking_number, "
                        + "count(distinct trim(po.tracking_number)) as tracking_count "
                        // Existing tables and new Whop tables may inherit different database defaults.
                        // Explicit collation on the Whop side resolves both JOIN and HAVING comparisons.
                        + "from whop_order w join platform_order po on po.platform_order_id=w.payment_id collate utf8mb4_general_ci "
                        + "join shop s on s.id=po.shop_id "
                        + "left join whop_shipment_sync ss on ss.account_id=w.account_id collate utf8mb4_general_ci "
                        + "and ss.payment_id=w.payment_id collate utf8mb4_general_ci "
                        + "where w.account_id=? and s.erp_code=? and po.erp_status='3' "
                        + "and po.tracking_number is not null and trim(po.tracking_number)<>'' "
                        + "group by w.payment_id "
                        + "having max(ss.sync_status) is null or max(ss.sync_status)<>'SYNCED' "
                        // Druid 1.2.19 accepts COLLATE on the column, but cannot parse it after MAX(...).
                        + "or min(trim(po.tracking_number))<>max(ss.tracking_number collate utf8mb4_general_ci) "
                        + "or max(trim(po.tracking_number))<>max(ss.tracking_number collate utf8mb4_general_ci) "
                        + "order by coalesce(max(ss.last_attempt_at), '1970-01-01'), w.payment_id limit ?",
                (rs, row) -> new WhopShipmentCandidate(rs.getString("payment_id"),
                        rs.getString("tracking_number"), rs.getInt("tracking_count")), accountId, shopName, limit);
    }

    public void begin(String account, WhopShipmentCandidate candidate, String key) {
        JSONObject request = new JSONObject();
        request.put("account_id", account);
        request.put("payment_id", candidate.getPaymentId());
        request.put("tracking_number", candidate.getTrackingNumber());
        jdbc.update("insert into whop_shipment_sync (account_id,payment_id,tracking_number,sync_status,"
                        + "idempotency_key,request_json,attempts,last_attempt_at) values (?,?,?,'PENDING',?,?,1,UTC_TIMESTAMP(3)) "
                        + "on duplicate key update tracking_number=values(tracking_number),sync_status='PENDING',"
                        + "idempotency_key=values(idempotency_key),request_json=values(request_json),response_json=null,"
                        + "last_error=null,synced_at=null,attempts=attempts+1,last_attempt_at=UTC_TIMESTAMP(3)",
                account, candidate.getPaymentId(), candidate.getTrackingNumber(), key, request.toJSONString());
    }

    public void synced(String account, String payment, JSONObject response) {
        jdbc.update("update whop_shipment_sync set sync_status='SYNCED',shipment_id=?,response_json=?,last_error=null,"
                        + "synced_at=UTC_TIMESTAMP(3) where account_id=? and payment_id=?",
                response.getString("id"), response.toJSONString(), account, payment);
    }

    public void failed(String account, String payment, String status, String reason, JSONObject response) {
        jdbc.update("update whop_shipment_sync set sync_status=?,last_error=?,response_json=? where account_id=? and payment_id=?",
                status, reason, response == null ? null : response.toJSONString(), account, payment);
    }
}
