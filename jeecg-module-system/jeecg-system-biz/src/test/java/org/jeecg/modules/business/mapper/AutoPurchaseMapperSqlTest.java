package org.jeecg.modules.business.mapper;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.jeecg.config.mybatis.MybatisPlusSaasConfig;
import org.jeecg.modules.business.entity.AutoPurchaseRun;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Exercise the real configured tenant SQL parser, rather than mocking mapper calls. */
class AutoPurchaseMapperSqlTest {
    private final Configuration configuration = new Configuration();
    private final TenantLineInnerInterceptor tenant = (TenantLineInnerInterceptor)
            new MybatisPlusSaasConfig().mybatisPlusInterceptor().getInterceptors().get(0);

    AutoPurchaseMapperSqlTest() { configuration.addMapper(AutoPurchaseMapper.class); }

    private String boundSql(String method) {
        AutoPurchaseRun run = new AutoPurchaseRun();
        run.setId("run"); run.setClientId("client"); run.setPurchaseId("purchase");
        run.setRecipient("test@example.com"); run.setMabangUsername("buyer"); run.setAutoMarkPaid(false);
        Map<String,Object> parameters = new HashMap<>();
        parameters.put("clientId", "client"); parameters.put("day", "2026-11-19");
        parameters.put("id", "run"); parameters.put("run", run);
        parameters.put("state", "DONE"); parameters.put("result", "{}");
        BoundSql sql = configuration.getMappedStatement(AutoPurchaseMapper.class.getName() + "." + method).getBoundSql(parameters);
        return sql.getSql();
    }

    @Test void eligibleSkuSelectionParsesWithConfiguredTenantInterceptor() {
        String sql = boundSql("eligibleSkus");
        assertDoesNotThrow(() -> tenant.parserSingle(sql, null));
    }

    @TestFactory Stream<DynamicTest> allAutomaticPurchaseStatementsParseWithTenantInterceptor() {
        return Stream.of("lockClient", "eligibleSkus", "findBlockingRun", "insert", "emailSent", "claimPlan", "finishPlan")
                .map(method -> DynamicTest.dynamicTest(method,
                        () -> assertDoesNotThrow(() -> tenant.parserSingle(boundSql(method), null))));
    }
}
