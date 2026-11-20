package org.jeecg.modules.business.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.jeecg.modules.business.vo.OrderKpi;
import org.jeecg.modules.business.vo.SkuDetail;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/** MySQL regression tests using only connection-local temporary fixture tables.
 * Reads use literal fixture CTEs: correlated queries cannot reopen a MySQL temporary table.
 */
@EnabledIfEnvironmentVariable(named = "QUERY_TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
public class QueryOptimizationMysqlTest {
    private Connection c;
    private Configuration config;
    private final List<String> tables = new ArrayList<>();
    private static final String NS = "org.jeecg.modules.business.mapper.";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 17, 12, 0);

    @BeforeEach void openFixtures() throws Exception {
        c = DriverManager.getConnection(System.getenv("QUERY_TEST_MYSQL_URL"), System.getenv("QUERY_TEST_MYSQL_USER"), System.getenv("QUERY_TEST_MYSQL_PASSWORD"));
        config = new Configuration(); config.setMapUnderscoreToCamelCase(true);
        for (String name : List.of("PlatformOrderMapper", "PlatformOrderContentMapper", "SkuListMabangMapper")) {
            Path path = Path.of("src/main/java/org/jeecg/modules/business/mapper/xml/" + name + ".xml");
            try (var in = Files.newInputStream(path)) { new XMLMapperBuilder(in, config, path.toString(), config.getSqlFragments()).parse(); }
        }
        table("shop", "id varchar(64), erp_code varchar(64)");
        table("platform_order", "id varchar(64), shop_id varchar(64), platform_order_id varchar(64), erp_status varchar(32), can_send varchar(32), order_time datetime(6), shipping_time datetime(6), shipping_invoice_number varchar(64), purchase_invoice_number varchar(64), status int, country varchar(64), create_time datetime(6)");
        table("platform_order_content", "id varchar(64), platform_order_id varchar(64), sku_id varchar(100), erp_status varchar(32), warehouse_name varchar(100), quantity int, create_time datetime(6)");
        table("sku", "id varchar(100), erp_code varchar(100), en_name varchar(100), zh_name varchar(100), image_source varchar(100)");
        table("sku_price", "id varchar(64), sku_id varchar(100), price decimal(12,2), unit int, threshold int, discounted_price decimal(12,2), currency_id varchar(64), date datetime(6)");
        table("sku_promotion_relation", "sku_id varchar(100), promotion_id varchar(64), promo_milestone int, quantity_purchased int, discount decimal(12,2)");
        table("third_party_stock", "sku_id varchar(100), quantity int, warehouse_name varchar(100)");
        table("third_party_stock_attribution", "platform_order_id varchar(64), sku_id varchar(100)");
        table("shipping_invoice", "invoice_number varchar(64), create_by varchar(64)");
        table("purchase_order", "invoice_number varchar(64), create_by varchar(64)");
    }
    @AfterEach void closeFixtures() throws Exception { if (c != null) c.close(); }
    private void table(String name, String columns) throws Exception {
        execute("CREATE TEMPORARY TABLE qopt_" + name + " (" + columns + ") DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci"); tables.add(name);
    }
    private void execute(String sql) throws Exception { try (Statement s = c.createStatement()) { s.setQueryTimeout(15); s.execute(sql); } }
    private void add(String table, String columns, Object... values) throws Exception {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO qopt_" + table + " (" + columns + ") VALUES (" + String.join(",", Collections.nCopies(values.length, "?")) + ")")) {
            for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]); s.executeUpdate();
        }
    }
    private String fixtureSql(String sql) throws Exception {
        List<String> snapshots = new ArrayList<>();
        for (String table : tables) {
            try (Statement statement = c.createStatement(); ResultSet rs = statement.executeQuery("SELECT * FROM qopt_" + table)) {
                ResultSetMetaData md = rs.getMetaData();
                List<String> selects = new ArrayList<>();
                do {
                    boolean hasRow = rs.next();
                    if (!hasRow && !selects.isEmpty()) break;
                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        String value = hasRow ? rs.getString(i) : null;
                        String literal = value == null ? "NULL" : "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
                        int type = md.getColumnType(i);
                        String cast = type == Types.INTEGER ? "SIGNED" : type == Types.DECIMAL ? "DECIMAL(20,6)"
                                : type == Types.TIMESTAMP ? "DATETIME(6)" : "CHAR CHARACTER SET utf8mb4";
                        columns.add("CAST(" + literal + " AS " + cast + ") AS `" + md.getColumnLabel(i) + "`");
                    }
                    selects.add("SELECT " + String.join(",", columns) + (hasRow ? "" : " WHERE 1=0"));
                    if (!hasRow) break;
                } while (true);
                snapshots.add(table + " AS (" + String.join(" UNION ALL ", selects) + ")");
            }
        }
        String ctes = String.join(", ", snapshots);
        sql = sql.replace("NOW()", "CAST('2026-11-17 12:00:00' AS DATETIME)").replace("UTC_TIMESTAMP()", "CAST('2026-11-17 12:00:00' AS DATETIME)");
        return "WITH " + ctes + (sql.trim().toUpperCase(Locale.ROOT).startsWith("WITH ") ? ", " + sql.trim().substring(5) : " " + sql);
    }
    private List<Map<String, Object>> query(String mapper, String name, Map<String, Object> p) throws Exception {
        MappedStatement ms = config.getMappedStatement(NS + mapper + "." + name); BoundSql b = ms.getBoundSql(p);
        try (PreparedStatement s = c.prepareStatement(fixtureSql(b.getSql()))) {
            s.setQueryTimeout(15); new DefaultParameterHandler(ms, p, b).setParameters(s);
            try (ResultSet rs = s.executeQuery()) {
                List<Map<String, Object>> out = new ArrayList<>();
                while (rs.next()) { Map<String, Object> row = new LinkedHashMap<>(); for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i)); out.add(row); } return out;
            }
        }
    }
    private int update(Map<String, Object> p) throws Exception {
        MappedStatement ms = config.getMappedStatement(NS + "SkuListMabangMapper.updateSkuIdBackfillBatch"); BoundSql b = ms.getBoundSql(p);
        String sql = b.getSql().replaceAll("\\bplatform_order_content\\b", "qopt_platform_order_content").replaceAll("\\bsku\\b", "qopt_sku");
        try (PreparedStatement s = c.prepareStatement(sql)) { new DefaultParameterHandler(ms, p, b).setParameters(s); return s.executeUpdate(); }
    }
    private List<?> mapped(String mapper, String name, Map<String, Object> p) throws Exception {
        MappedStatement original = config.getMappedStatement(NS + mapper + "." + name);
        BoundSql source = original.getBoundSql(p);
        BoundSql bound = new BoundSql(config, fixtureSql(source.getSql()), source.getParameterMappings(), p);
        for (ParameterMapping mapping : source.getParameterMappings()) {
            if (source.hasAdditionalParameter(mapping.getProperty()))
                bound.setAdditionalParameter(mapping.getProperty(), source.getAdditionalParameter(mapping.getProperty()));
        }
        String id = "fixtureMapped." + mapper + name;
        config.addMappedStatement(new MappedStatement.Builder(config, id, ignored -> bound, SqlCommandType.SELECT)
                .resultMaps(original.getResultMaps()).timeout(15).build());
        config.setEnvironment(new Environment("fixture", new JdbcTransactionFactory(), new SingleConnectionDataSource(c, true)));
        try (SqlSession session = new SqlSessionFactoryBuilder().build(config).openSession(true)) { return session.selectList(id, p); }
    }
    private void sku(String id, String erp) throws Exception { add("sku", "id,erp_code", id, erp); }
    private void order(String id, String shop, String status, String canSend, LocalDateTime time) throws Exception {
        add("platform_order", "id,shop_id,platform_order_id,erp_status,can_send,order_time,create_time,country", id, shop, "external-" + id, status, canSend, time, time, "France");
    }
    private void content(String id, String order, String sku, String status, String warehouse) throws Exception {
        add("platform_order_content", "id,platform_order_id,sku_id,erp_status,warehouse_name,quantity,create_time", id, order, sku, status, warehouse, 1, NOW);
    }
    private Set<Object> ids(List<Map<String, Object>> rows, String key) { return rows.stream().map(r -> r.get(key)).collect(Collectors.toSet()); }

    @Test void abnormalOrdersRetainCancellationWarehouseAndDateRules() throws Exception {
        add("shop", "id,erp_code", "shop", "SHOP"); add("shop", "id,erp_code", "other", "OTHER"); sku("normal", "NORMAL"); sku("target", "TARGET");
        for (String id : List.of("ready", "bad", "cancelled", "wrong-warehouse", "paris", "null-warehouse", "empty")) order(id, "shop", "1", "2", NOW.minusDays(1));
        order("old", "shop", "1", "2", NOW.minusDays(6)); order("wrong-shop", "other", "1", "2", NOW.minusDays(1)); order("normal", "shop", "1", "1", NOW); order("shipped", "shop", "3", "2", NOW);
        content("c-ready", "ready", "normal", "1", "China"); content("c-bad", "bad", null, "1", "China"); content("c-cancelled", "cancelled", "missing", "5", "China");
        content("c-wrong", "wrong-warehouse", "target", "1", "China"); content("c-paris", "paris", "target", "1", "法国巴黎仓库-唯客路"); content("c-null", "null-warehouse", "target", "1", null);
        assertEquals(Set.of("ready", "cancelled", "paris", "null-warehouse", "empty"), ids(query("PlatformOrderMapper", "findReadyAbnormalOrdersByShopIds", Map.of("shopIds", List.of("shop"), "skus", List.of("TARGET"))), "id"));
        assertTrue(query("PlatformOrderMapper", "findReadyAbnormalOrdersByShopIds", Map.of("shopIds", List.of(), "skus", List.of())).isEmpty());
        assertTrue(query("PlatformOrderMapper", "findReadyAbnormalOrdersWithSkus", Map.of("skus", List.of())).isEmpty());
        assertEquals(Set.of("paris"), ids(query("PlatformOrderMapper", "findReadyAbnormalOrdersWithSkus", Map.of("skus", List.of("TARGET"))), "platform_order_id"));
    }
    @Test void promotionRetainsEffectivePricesTiesPromotionsAndSalesOrder() throws Exception {
        sku("a", "A"); sku("b", "B"); sku("unrequested", "C");
        for (String price : List.of("current-1", "current-2")) add("sku_price", "id,sku_id,price,date", price, "a", 5, NOW);
        add("sku_price", "id,sku_id,price,date", "old", "a", 4, NOW.minusDays(1)); add("sku_price", "id,sku_id,price,date", "future", "a", 99, NOW.plusDays(1)); add("sku_price", "id,sku_id,price,date", "b-price", "b", 3, NOW);
        for (String promo : List.of("promo-1", "promo-2")) add("sku_promotion_relation", "sku_id,promotion_id,promo_milestone,quantity_purchased,discount", "a", promo, 1, 0, new java.math.BigDecimal("0.10"));
        order("sales", "shop", "1", "1", NOW); execute("UPDATE qopt_platform_order SET status=2"); content("sale-a", "sales", "a", "1", "China"); execute("UPDATE qopt_platform_order_content SET quantity=10"); content("sale-b", "sales", "b", "1", "China");
        List<Map<String, Object>> rows = query("PlatformOrderContentMapper", "searchSkuDetail", Map.of("list", List.of("a", "b")));
        assertEquals(5, rows.size()); assertEquals("b", rows.get(4).get("sku_id")); assertEquals(Set.of("current-1", "current-2", "b-price"), ids(rows, "price_id"));
        assertEquals(Set.of("promo-1", "promo-2"), ids(rows.stream().filter(r -> "a".equals(r.get("sku_id"))).collect(Collectors.toList()), "promotion_id"));
        List<?> details = mapped("PlatformOrderContentMapper", "searchSkuDetail", Map.of("list", List.of("a", "b")));
        assertEquals(Set.of("a", "b"), details.stream().map(r -> ((SkuDetail) r).getSkuId()).collect(Collectors.toSet()));
        assertTrue(details.stream().allMatch(r -> ((SkuDetail) r).getPrice().getPrice().signum() > 0));
        assertTrue(query("PlatformOrderContentMapper", "searchSkuDetail", Map.of("list", List.of())).isEmpty());
    }
    @Test void backfillPagesEqualTimestampsWithoutOverwritingConcurrentImports() throws Exception {
        sku("resolved-a", "CODE-A"); sku("resolved-b", "CODE-B"); sku("same", "same"); sku("resolved-prefix", "123-CODE");
        for (String id : List.of("a", "b", "c")) content(id, "order", "CODE-A", "1", "China");
        content("unmapped", "order", "NO-MAPPING", "1", "China"); content("prefix", "order", "123-CODE", "1", "China"); content("noop", "order", "same", "1", "China"); content("old", "order", "CODE-A", "1", "China");
        execute("UPDATE qopt_platform_order_content SET create_time='2025-01-01' WHERE id='old'");
        Map<String, Object> p = new HashMap<>(); p.put("cutoff", NOW.minusMonths(6)); p.put("upper", NOW); p.put("afterTime", null); p.put("afterId", null); p.put("limit", 2);
        List<Map<String, Object>> first = query("SkuListMabangMapper", "findSkuIdBackfillBatch", p); assertEquals(List.of("a", "b"), first.stream().map(r -> r.get("id")).collect(Collectors.toList()));
        execute("UPDATE qopt_platform_order_content SET sku_id='CODE-B' WHERE id='b'");
        assertEquals(1, update(Map.of("rows", first, "cutoff", NOW.minusMonths(6), "upper", NOW)));
        p.put("afterTime", NOW); p.put("afterId", "b"); List<Map<String, Object>> second = query("SkuListMabangMapper", "findSkuIdBackfillBatch", p); assertEquals(Set.of("c"), ids(second, "id"));
        assertEquals(1, update(Map.of("rows", second, "cutoff", NOW.minusMonths(6), "upper", NOW))); assertEquals(0, update(Map.of("rows", List.of(), "cutoff", NOW.minusMonths(6), "upper", NOW)));
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT sku_id FROM qopt_platform_order_content WHERE id='b'")) { assertTrue(rs.next()); assertEquals("CODE-B", rs.getString(1)); }
        content("future", "order", "CODE-A", "1", "China");
        execute("UPDATE qopt_platform_order_content SET create_time='2026-11-18 12:00:00' WHERE id='future'");
        Object watermark = query("SkuListMabangMapper", "skuIdBackfillUpper", Map.of()).get(0).values().iterator().next();
        assertEquals(NOW.plusDays(1), watermark instanceof Timestamp ? ((Timestamp) watermark).toLocalDateTime() : watermark);
        p.put("upper", NOW.plusDays(1)); p.put("afterId", "c");
        assertEquals(Set.of("future"), ids(query("SkuListMabangMapper", "findSkuIdBackfillBatch", p), "id"));
    }
    @Test void inventoryRetainsDuplicateRowsWarehouseAndNotInNullRules() throws Exception {
        add("shop", "id,erp_code", "shop", "SHOP"); sku("sku", "SKU"); order("ready", "shop", "1", "1", NOW); order("assigned", "shop", "1", "1", NOW);
        content("one", "ready", "sku", "1", "China"); content("two", "ready", "sku", "1", "China"); content("done", "assigned", "sku", "1", "China"); content("null-warehouse", "ready", "sku", "1", null);
        add("third_party_stock", "sku_id,quantity,warehouse_name", "sku", 3, "Paris"); add("third_party_stock_attribution", "platform_order_id,sku_id", "external-assigned", "sku");
        Map<String, Object> p = Map.of("countries", List.of("France"), "shopCodes", List.of("SHOP"), "startDateTime", NOW.minusDays(1));
        List<Map<String, Object>> rows = query("PlatformOrderMapper", "fetchThirdPartyStockAttributionParams", p); assertEquals(2, rows.size()); assertEquals(Set.of("external-ready"), ids(rows, "platform_order_id"));
        add("third_party_stock_attribution", "platform_order_id,sku_id", null, "sku"); assertTrue(query("PlatformOrderMapper", "fetchThirdPartyStockAttributionParams", p).isEmpty());
        assertTrue(query("PlatformOrderMapper", "fetchThirdPartyStockAttributionParams", Map.of("countries", List.of(), "shopCodes", List.of(), "startDateTime", NOW)).isEmpty());
    }
    @Test void kpiCountsInvoiceOverlapOnceAndPreservesAllDataVisibility() throws Exception {
        add("shipping_invoice", "invoice_number,create_by", "A", "alice"); add("shipping_invoice", "invoice_number,create_by", "A", "alice"); add("purchase_order", "invoice_number,create_by", "A", "alice"); add("purchase_order", "invoice_number,create_by", "P", "alice");
        order("processed", "shop", "4", "1", NOW); order("processing", "shop", "2", "1", NOW); order("purchase-only", "shop", "3", "1", NOW); order("cross-type", "shop", "4", "1", NOW); order("bob", "shop", "4", "1", NOW); order("old-shipment", "shop", "4", "1", NOW);
        execute("UPDATE qopt_platform_order SET shipping_time=order_time,shipping_invoice_number='A' WHERE id IN ('processed','processing','old-shipment')"); execute("UPDATE qopt_platform_order SET purchase_invoice_number='P' WHERE id IN ('processing','purchase-only')"); execute("UPDATE qopt_platform_order SET shipping_invoice_number='P',shipping_time=order_time WHERE id='cross-type'"); execute("UPDATE qopt_platform_order SET shipping_invoice_number='B',shipping_time=order_time WHERE id='bob'"); execute("UPDATE qopt_platform_order SET shipping_time='2025-01-01' WHERE id='old-shipment'");
        Map<String, Object> p = new HashMap<>(Map.of("start", NOW.minusDays(1), "end", NOW.plusDays(1), "username", "alice", "showAllData", false));
        assertCounts(p, 2, 2); p.put("showAllData", true); assertCounts(p, 3, 1); p.put("showAllData", false); p.put("username", "nobody"); assertCounts(p, 0, 0);
        p.put("username", "alice");
        OrderKpi kpi = (OrderKpi) mapped("PlatformOrderMapper", "countPlatformOrders", p).get(0);
        assertEquals(2L, kpi.getProcessed()); assertEquals(2L, kpi.getProcessing());
    }
    private void assertCounts(Map<String, Object> p, int processed, int processing) throws Exception {
        Map<String, Object> r = query("PlatformOrderMapper", "countPlatformOrders", p).get(0); assertEquals(processed, ((Number) r.get("processed")).intValue()); assertEquals(processing, ((Number) r.get("processing")).intValue());
    }
}
