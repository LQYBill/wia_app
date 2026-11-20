package org.jeecg.modules.business.domain.job;

import org.jeecg.modules.business.domain.api.mabang.getorderlist.OrderItem;
import org.junit.jupiter.api.Test;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddPortraitTubeJobTest {

    @Test
    public void applyCanvasRules_prefixRuleMatches() {
        AddPortraitTubeJob.CanvasCounts counts = new AddPortraitTubeJob.CanvasCounts();
        boolean matched = AddPortraitTubeJob.applyCanvasRules("JJ2501-ABC", 2, counts);

        assertTrue(matched);
        assertEquals(2, counts.getCanvas50Count());
    }

    @Test
    public void applyCanvasRules_regexRuleMatches() {
        AddPortraitTubeJob.CanvasCounts counts = new AddPortraitTubeJob.CanvasCounts();
        boolean matched = AddPortraitTubeJob.applyCanvasRules("JJ314XX00YY-JCH", 3, counts);

        assertTrue(matched);
        assertEquals(3, counts.getCanvasNew36Count());
    }

    @Test
    public void applyCanvasRules_unknownSkuReturnsFalse() {
        AddPortraitTubeJob.CanvasCounts counts = new AddPortraitTubeJob.CanvasCounts();
        boolean matched = AddPortraitTubeJob.applyCanvasRules("UNKNOWN-SKU", 1, counts);

        assertFalse(matched);
        assertEquals(0, counts.getCanvas50Count());
        assertEquals(0, counts.getCanvasNew36Count());
        assertEquals(0, counts.getCanvasUV50Count());
    }

    @Test
    public void collectCanvasCounts_usesRuleMapping() {
        AddPortraitTubeJob job = new AddPortraitTubeJob();
        OrderItem item50 = new OrderItem();
        item50.setStatus("2");
        item50.setErpCode("JJ2501-TEST");
        item50.setQuantity(1);
        OrderItem itemUv = new OrderItem();
        itemUv.setStatus("2");
        itemUv.setErpCode("JJ989AA02BB-JCH");
        itemUv.setQuantity(2);
        List<OrderItem> items = Arrays.asList(item50, itemUv);

        AddPortraitTubeJob.CanvasCounts counts = invokeCollectCanvasCounts(job, items);
        assertEquals(1, counts.getCanvas50Count());
        assertEquals(2, counts.getCanvasUV50Count());
    }

    @Test
    public void smallAndMediumSizes_packAtMostThreeCanvasesPerTube() throws Exception {
        for (String sku : Arrays.asList("JJ2502-TEST", "JJ2002-TEST", "JJ2500-TEST", "JJ2000-TEST",
                "JJ314XX00YY-JCH", "JJ989AA00BB-JCH", "JJ989AA01BB-JCH")) {
            for (int quantity : new int[]{0, 1, 2, 3, 4, 6, 7, 10}) {
                assertEquals(quantity == 0 ? Collections.emptySet()
                                : Collections.singleton(Pair.of("PJ95430040-WIA", (quantity + 2) / 3)),
                        findTubes(item(sku, quantity)).getRight());
            }
        }
        for (String sku : Arrays.asList("JJ2501-TEST", "JJ2001-TEST", "JJ314XX01YY-JCH", "JJ989AA02BB-JCH")) {
            for (int quantity : new int[]{0, 1, 2, 3, 4, 6, 7, 10}) {
                assertEquals(quantity == 0 ? Collections.emptySet()
                                : Collections.singleton(Pair.of("PJ95530040-WIA", (quantity + 2) / 3)),
                        findTubes(item(sku, quantity)).getRight());
            }
        }
    }

    @Test
    public void size56_packsGroupsOfThreeAndRemainingOneOrTwo() throws Exception {
        for (int quantity = 1; quantity <= 10; quantity++) {
            HashSet<Pair<String, Integer>> expected = new HashSet<>();
            if (quantity / 3 > 0) expected.add(Pair.of("PJ349600048-JCH", quantity / 3));
            if (quantity % 3 > 0) expected.add(Pair.of("PJ349600045-JCH", 1));
            assertEquals(expected, findTubes(item("JJ314XX02YY-JCH", quantity)).getRight());
        }
    }

    @Test
    public void mixedSizes_aggregateEachGroupBeforePacking() throws Exception {
        HashSet<Pair<String, Integer>> expected = new HashSet<>(Arrays.asList(
                Pair.of("PJ95430040-WIA", 6), Pair.of("PJ95530040-WIA", 4),
                Pair.of("PJ349600048-JCH", 1), Pair.of("PJ349600045-JCH", 1)));
        assertEquals(expected, findTubes(
                item("JJ2502-TEST", 10), item("JJ2500-TEST", 4), item("JJ314XX00YY-JCH", 2),
                item("JJ2501-TEST", 4), item("JJ314XX01YY-JCH", 6),
                item("JJ314XX02AA-JCH", 2), item("JJ314XX02BB-JCH", 3)).getRight());
    }

    @Test
    public void retiredTubes_areRecognizedForReplacement() throws Exception {
        for (String sku : Arrays.asList("PJ95310032-WIA", "PJ349600032-JCH", "PJ95330045-WIA",
                "PJ95530032-WIA", "PJ95430032-WIA", "PJ349500032-JCH", "PJ349400032-JCH",
                "PJ95330032-WIA", "PJ349500045-JCH", "PJ349400045-JCH")) {
            Pair<HashSet<Pair<String, Integer>>, HashSet<Pair<String, Integer>>> tubes =
                    findTubes(item(sku, 2), item("JJ2502-TEST", 3));
            assertEquals(Collections.singleton(Pair.of(sku, 2)), tubes.getLeft());
            assertEquals(Collections.singleton(Pair.of("PJ95430040-WIA", 1)), tubes.getRight());
        }
    }

    @Test
    public void adequateTubes_areStableAndObsoleteItemsAreIgnored() throws Exception {
        for (String[] mapping : new String[][]{
                {"JJ2502-TEST", "PJ95430040-WIA"}, {"JJ2501-TEST", "PJ95530040-WIA"},
                {"JJ314XX02YY-JCH", "PJ349600045-JCH"}}) {
            Pair<HashSet<Pair<String, Integer>>, HashSet<Pair<String, Integer>>> tubes =
                    findTubes(item(mapping[0], 1), item(mapping[1], 1));
            assertEquals(tubes.getLeft(), tubes.getRight());
        }
        OrderItem obsolete = item("JJ314XX02YY-JCH", 3);
        obsolete.setStatus("4");
        OrderItem obsoleteTube = item("PJ95310032-WIA", 1);
        obsoleteTube.setStatus("4");
        Pair<HashSet<Pair<String, Integer>>, HashSet<Pair<String, Integer>>> tubes =
                findTubes(obsolete, obsoleteTube, item("UNKNOWN-SKU", 1));
        assertTrue(tubes.getLeft().isEmpty());
        assertTrue(tubes.getRight().isEmpty());
        assertTrue(findTubes().getRight().isEmpty());
    }

    @Test
    public void triptychs_useOneDedicatedTubePerSet() throws Exception {
        for (String[] mapping : new String[][]{
                {"JJ25VF00", "PJ95430040-WIA"}, {"JJ25VF01", "PJ95530040-WIA"},
                {"JJ25VF02", "PJ349600048-JCH"}}) {
            for (int quantity : new int[]{0, 1, 2, 4}) {
                assertEquals(quantity == 0 ? Collections.emptySet()
                                : Collections.singleton(Pair.of(mapping[1], quantity)),
                        findTubes(item(mapping[0] + "ABC-WIA", quantity)).getRight());
            }
        }
        assertTrue(findTubes(item("JJ25VF03ABC-WIA", 1)).getRight().isEmpty());
    }

    @Test
    public void triptychs_andOrdinaryCanvases_addTubeQuantitiesWithoutSharing() throws Exception {
        HashSet<Pair<String, Integer>> expected = new HashSet<>(Arrays.asList(
                Pair.of("PJ95430040-WIA", 4), Pair.of("PJ95530040-WIA", 3),
                Pair.of("PJ349600048-JCH", 3), Pair.of("PJ349600045-JCH", 1)));
        assertEquals(expected, findTubes(
                item("JJ25VF00AAA-WIA", 1), item("JJ25VF00BBB-WIA", 2),
                item("JJ25VF01AAA-WIA", 2), item("JJ25VF02AAA-WIA", 2),
                item("JJ2502-TEST", 1), item("JJ2501-TEST", 2),
                item("JJ314XX02YY-JCH", 4)).getRight());
    }

    @Test
    public void triptychTubes_areStableAndObsoleteSetsAreIgnored() throws Exception {
        for (String[] mapping : new String[][]{
                {"JJ25VF00AAA-WIA", "PJ95430040-WIA"}, {"JJ25VF01AAA-WIA", "PJ95530040-WIA"},
                {"JJ25VF02AAA-WIA", "PJ349600048-JCH"}}) {
            Pair<HashSet<Pair<String, Integer>>, HashSet<Pair<String, Integer>>> tubes =
                    findTubes(item(mapping[0], 2), item(mapping[1], 2));
            assertEquals(tubes.getLeft(), tubes.getRight());
            OrderItem obsolete = item(mapping[0], 2);
            obsolete.setStatus("4");
            assertTrue(findTubes(obsolete).getRight().isEmpty());
        }
    }

    @Test
    public void letterSkus_matchSizeCodesAndPackAtMostThreePerTube() throws Exception {
        for (String letters : Arrays.asList("AB", "XX", "YZ", "ab")) {
            for (String[] mapping : new String[][]{
                    {"00", "PJ95430040-WIA"}, {"01", "PJ95430040-WIA"}, {"02", "PJ95530040-WIA"}}) {
                for (String suffix : Arrays.asList("", "123-WIA", "-JCH")) {
                    for (int quantity : new int[]{0, 1, 3, 4, 7}) {
                        assertEquals(quantity == 0 ? Collections.emptySet()
                                        : Collections.singleton(Pair.of(mapping[1], (quantity + 2) / 3)),
                                findTubes(item("JJ25" + letters + mapping[0] + suffix, quantity)).getRight());
                    }
                }
            }
        }
    }

    @Test
    public void letterSkus_rejectInvalidPrefixesAndSizeCodes() {
        for (String sku : Arrays.asList("JJ25A00-WIA", "JJ25ABC00-WIA", "JJ25A100-WIA",
                "JJ251A00-WIA", "JJ25AB03-WIA", "OTHER-JJ25AB00-WIA")) {
            assertFalse(AddPortraitTubeJob.applyCanvasRules(sku, 1, new AddPortraitTubeJob.CanvasCounts()));
        }
    }

    @Test
    public void letterSkus_shareOrdinaryTubesButKeepTriptychsSeparate() throws Exception {
        HashSet<Pair<String, Integer>> expected = new HashSet<>(Arrays.asList(
                Pair.of("PJ95430040-WIA", 6), Pair.of("PJ95530040-WIA", 3),
                Pair.of("PJ349600048-JCH", 2)));
        assertEquals(expected, findTubes(
                item("JJ25AB00-WIA", 2), item("JJ25CD01-WIA", 2), item("JJ2502-TEST", 1),
                item("JJ25EF02-WIA", 2), item("JJ2501-TEST", 1),
                item("JJ25VF00-WIA", 4), item("JJ25VF01-WIA", 2), item("JJ25VF02-WIA", 2)).getRight());
        OrderItem obsolete = item("JJ25AB00-WIA", 4);
        obsolete.setStatus("4");
        assertTrue(findTubes(obsolete).getRight().isEmpty());
    }

    private OrderItem item(String sku, int quantity) {
        OrderItem item = new OrderItem();
        item.setErpCode(sku);
        item.setQuantity(quantity);
        item.setStatus("2");
        return item;
    }

    @SuppressWarnings("unchecked")
    private Pair<HashSet<Pair<String, Integer>>, HashSet<Pair<String, Integer>>> findTubes(OrderItem... items)
            throws Exception {
        java.lang.reflect.Method method = AddPortraitTubeJob.class.getDeclaredMethod(
                "findCurrentAndAdequateTubes", List.class);
        method.setAccessible(true);
        return (Pair<HashSet<Pair<String, Integer>>, HashSet<Pair<String, Integer>>>)
                method.invoke(new AddPortraitTubeJob(), Arrays.asList(items));
    }

    private AddPortraitTubeJob.CanvasCounts invokeCollectCanvasCounts(AddPortraitTubeJob job, List<OrderItem> items) {
        try {
            var method = AddPortraitTubeJob.class.getDeclaredMethod("collectCanvasCounts", List.class, java.util.HashSet.class);
            method.setAccessible(true);
            return (AddPortraitTubeJob.CanvasCounts) method.invoke(job, items, new java.util.HashSet<>());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
