package org.jeecg.modules.business.service.impl;

import org.jeecg.modules.business.domain.api.whop.WhopShipmentCandidate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL 8 regression: temporary tables disappear when this dedicated connection closes. */
@EnabledIfEnvironmentVariable(named = "WHOP_TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
class WhopShipmentSyncRepositoryMysqlTest {
    @Test void newWhopTablesCanJoinExistingGeneralCiOrders() throws Exception {
        verifyMixedCollations("utf8mb4_0900_ai_ci", "utf8mb4_0900_ai_ci");
    }

    @Test void shipmentTableCanDifferFromBothOrderTables() throws Exception {
        verifyMixedCollations("utf8mb4_general_ci", "utf8mb4_0900_ai_ci");
    }

    private void verifyMixedCollations(String orderCollation, String shipmentCollation) throws Exception {
        String user = System.getenv("WHOP_TEST_MYSQL_USER");
        String password = System.getenv("WHOP_TEST_MYSQL_PASSWORD");
        try (Connection connection = DriverManager.getConnection(System.getenv("WHOP_TEST_MYSQL_URL"),
                user == null ? "root" : user, password == null ? "" : password)) {
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("create temporary table shop (id varchar(64), erp_code varchar(64)) "
                    + "default character set utf8mb4 collate utf8mb4_general_ci");
            jdbc.execute("create temporary table platform_order (platform_order_id varchar(64), shop_id varchar(64), "
                    + "tracking_number varchar(255), erp_status varchar(8)) "
                    + "default character set utf8mb4 collate utf8mb4_general_ci");
            jdbc.execute("create temporary table whop_order (account_id varchar(64), payment_id varchar(64)) "
                    + "default character set utf8mb4 collate " + orderCollation);
            jdbc.execute("create temporary table whop_shipment_sync (account_id varchar(64), payment_id varchar(64), "
                    + "tracking_number varchar(255), sync_status varchar(32), last_attempt_at datetime) "
                    + "default character set utf8mb4 collate " + shipmentCollation);
            jdbc.update("insert into shop values ('shop1','TID-Whop')");
            jdbc.update("insert into whop_order values ('biz_test','pay_test')");
            jdbc.update("insert into platform_order values ('pay_test','shop1','TRACK1','3')");
            jdbc.update("insert into whop_shipment_sync values ('biz_test','pay_test','TRACK1','FAILED','2026-01-01')");

            // Prove the fixture reproduces error 1267, rather than merely exercising a mock.
            String legacyJoin = "utf8mb4_0900_ai_ci".equals(orderCollation)
                    ? "select w.payment_id from whop_order w join platform_order po on po.platform_order_id=w.payment_id"
                    : "select w.payment_id from whop_order w join whop_shipment_sync ss on ss.account_id=w.account_id";
            DataAccessException failure = assertThrows(DataAccessException.class, () -> jdbc.queryForList(legacyJoin));
            assertEquals(1267, ((SQLException) failure.getMostSpecificCause()).getErrorCode());

            WhopShipmentSyncRepository repository = new WhopShipmentSyncRepository(jdbc);
            List<WhopShipmentCandidate> candidates = repository.candidates("biz_test", "TID-Whop", 100);
            assertEquals(1, candidates.size());
            assertEquals("TRACK1", candidates.get(0).getTrackingNumber());

            jdbc.update("update whop_shipment_sync set sync_status='SYNCED'");
            assertTrue(repository.candidates("biz_test", "TID-Whop", 100).isEmpty());

            // Also exercise HAVING comparisons between differently collated tracking columns.
            jdbc.update("update platform_order set tracking_number='TRACK2'");
            assertEquals("TRACK2", repository.candidates("biz_test", "TID-Whop", 100).get(0).getTrackingNumber());
            jdbc.update("insert into platform_order values ('pay_test','shop1','TRACK3','3')");
            assertEquals(2, repository.candidates("biz_test", "TID-Whop", 100).get(0).getTrackingCount());
        }
    }
}
