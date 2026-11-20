package org.jeecg.modules.business.service.impl;

import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.spi.MySqlWallProvider;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.api.whop.WhopShipmentCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Use the project's real Druid parser/filter rules; a mock database cannot catch parser rejections. */
class WhopShipmentSyncRepositoryWallTest {
    @Test void candidateQueryPassesMysqlWallWithTheFilterEnabled() {
        CapturingJdbc jdbc = new CapturingJdbc();
        new WhopShipmentSyncRepository(jdbc).candidates("biz_test", "TID-Whop", 100);
        assertEquals(1, jdbc.statements.size());
        assertAllowed(jdbc.statements.get(0));
    }

    @Test void requestAndResultWritesAlsoPassMysqlWall() {
        CapturingJdbc jdbc = new CapturingJdbc();
        WhopShipmentSyncRepository repository = new WhopShipmentSyncRepository(jdbc);
        repository.begin("biz_test", new WhopShipmentCandidate("pay_test", "TRACK1", 1), "request-key");
        JSONObject response = new JSONObject();
        response.put("id", "ship_test");
        repository.synced("biz_test", "pay_test", response);
        repository.failed("biz_test", "pay_test", "FAILED", "Test failure", null);
        assertEquals(3, jdbc.statements.size());
        jdbc.statements.forEach(this::assertAllowed);
    }

    private void assertAllowed(String sql) {
        var result = new MySqlWallProvider(new WallConfig()).check(sql);
        assertFalse(result.isSyntaxError(), () -> "Druid rejected SQL: " + result.getViolations());
        assertTrue(result.getViolations().isEmpty(), () -> "Druid wall violations: " + result.getViolations());
    }

    private static class CapturingJdbc extends JdbcTemplate {
        private final List<String> statements = new ArrayList<>();
        @Override public <T> List<T> query(String sql, RowMapper<T> mapper, Object... args) {
            statements.add(sql);
            return Collections.emptyList();
        }
        @Override public int update(String sql, Object... args) {
            statements.add(sql);
            return 1;
        }
    }
}
