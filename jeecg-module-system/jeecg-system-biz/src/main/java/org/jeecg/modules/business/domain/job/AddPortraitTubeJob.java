package org.jeecg.modules.business.domain.job;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ChangeOrderRequest;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ChangeOrderRequestBody;
import org.jeecg.modules.business.domain.api.mabang.dochangeorder.ChangeOrderResponse;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.*;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class AddPortraitTubeJob implements Job {

    private static final Integer DEFAULT_NUMBER_OF_DAYS = 5;
    private static final int MAXIMUM_CANVAS_IN_TUBE = 3;
    private static final int MAXIMUM_56_CANVAS_IN_TUBE = 3;

    private static final List<String> DEFAULT_SHOPS = Arrays.asList("JCH3", "JCH4", "JCH5");
    private static final Integer DEFAULT_NUMBER_OF_THREADS = 10;
    private static final String TUBE_40_SKU = "PJ95430040-WIA";
    private static final String TUBE_50_SKU = "PJ95530040-WIA";
    private static final String TUBE_60_SKU_SINGLE_DOUBLE = "PJ349600045-JCH";
    private static final String TUBE_60_SKU_TREBLE = "PJ349600048-JCH";

    // Include retired tubes so existing order items can be removed when applying the new rules.
    private static final List<String> TUBE_SKUS = Arrays.asList(
            "PJ95310032-WIA", "PJ349600032-JCH", "PJ95330045-WIA", "PJ95530032-WIA", "PJ95430032-WIA",
            "PJ349500032-JCH", "PJ349400032-JCH", "PJ95330032-WIA", "PJ349500045-JCH", "PJ349400045-JCH",
            TUBE_40_SKU, TUBE_50_SKU, TUBE_60_SKU_SINGLE_DOUBLE, TUBE_60_SKU_TREBLE);
    private static final String PREFIX_50_CANVAS = "JJ2501";
    private static final String PREFIX_50_CANVAS_CHROME = "JJ2001";
    private static final String PREFIX_40_CANVAS = "JJ2500";
    private static final String PREFIX_40_CANVAS_CHROME = "JJ2000";
    private static final String PREFIX_30_CANVAS = "JJ2502";
    private static final String PREFIX_30_CANVAS_CHROME = "JJ2002";
    // 亚麻棉
    private static final String REGEX_NEW_56_CANVAS = "JJ314.*02.*-JCH";
    private static final String REGEX_NEW_46_CANVAS = "JJ314.*01.*-JCH";
    private static final String REGEX_NEW_36_CANVAS = "JJ314.*00.*-JCH";
    // UV
    private static final String REGEX_UV_30_CANVAS = "JJ989.*00.*-JCH";
    private static final String REGEX_UV_40_CANVAS = "JJ989.*01.*-JCH";
    private static final String REGEX_UV_50_CANVAS = "JJ989.*02.*-JCH";

    /**
     * SKU-to-canvas rule mapping. The order matters: prefix rules should be evaluated before regex rules.
     */
    enum CanvasSkuRule {
        // Each triptych set requires its own tube.
        PREFIX_TRIPTYCH_00("JJ25VF00", null) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.triptych40TubeCount += quantity;
            }
        },
        PREFIX_TRIPTYCH_01("JJ25VF01", null) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.triptych50TubeCount += quantity;
            }
        },
        PREFIX_TRIPTYCH_02("JJ25VF02", null) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.triptych60TubeCount += quantity;
            }
        },
        // Prefix rules for standard canvas SKUs
        PREFIX_50(PREFIX_50_CANVAS, null) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvas50Count += quantity;
            }
        },
        PREFIX_50_CHROME(PREFIX_50_CANVAS_CHROME, null) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvas50Count += quantity;
            }
        },
        PREFIX_40(PREFIX_40_CANVAS, null) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvas40Count += quantity;
            }
        },
        PREFIX_40_CHROME(PREFIX_40_CANVAS_CHROME, null) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvas40Count += quantity;
            }
        },
        PREFIX_30(PREFIX_30_CANVAS, null) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvas30Count += quantity;
            }
        },
        PREFIX_30_CHROME(PREFIX_30_CANVAS_CHROME, null) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvas30Count += quantity;
            }
        },
        // Regex rules for special canvases
        REGEX_NEW_36(null, REGEX_NEW_36_CANVAS) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvasNew36Count += quantity;
            }
        },
        REGEX_NEW_46(null, REGEX_NEW_46_CANVAS) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvasNew46Count += quantity;
            }
        },
        REGEX_NEW_56(null, REGEX_NEW_56_CANVAS) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvasNew56Count += quantity;
            }
        },
        REGEX_UV_30(null, REGEX_UV_30_CANVAS) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvasUV30Count += quantity;
            }
        },
        REGEX_UV_40(null, REGEX_UV_40_CANVAS) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvasUV40Count += quantity;
            }
        },
        REGEX_UV_50(null, REGEX_UV_50_CANVAS) {
            @Override
            void apply(CanvasCounts counts, int quantity) {
                counts.canvasUV50Count += quantity;
            }
        };

        private final String prefix;
        private final String regex;

        CanvasSkuRule(String prefix, String regex) {
            this.prefix = prefix;
            this.regex = regex;
        }

        boolean matches(String sku) {
            if (prefix != null) {
                return sku.startsWith(prefix);
            }
            return sku.matches(regex);
        }

        abstract void apply(CanvasCounts counts, int quantity);
    }

    static boolean applyCanvasRules(String sku, int quantity, CanvasCounts counts) {
        for (CanvasSkuRule rule : CanvasSkuRule.values()) {
            if (rule.matches(sku)) {
                rule.apply(counts, quantity);
                return true;
            }
        }
        return false;
    }

    @Autowired
    private IPlatformOrderService platformOrderService;

    private static class JobParams {
        private final LocalDateTime startDateTime;
        private final LocalDateTime endDateTime;
        private final List<String> shops;

        private JobParams(LocalDateTime startDateTime, LocalDateTime endDateTime, List<String> shops) {
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
            this.shops = shops;
        }
    }

    private JobParams resolveJobParams(JobExecutionContext context) {
        LocalDateTime endDateTime = LocalDateTime.now();
        LocalDateTime startDateTime = endDateTime.minusDays(DEFAULT_NUMBER_OF_DAYS);
        List<String> shops = DEFAULT_SHOPS;
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String parameter = ((String) jobDataMap.get("parameter"));
        if (parameter != null) {
            try {
                JSONObject jsonObject = new JSONObject(parameter);
                if (!jsonObject.isNull("startDateTime")) {
                    String startDateStr = jsonObject.getString("startDateTime");
                    startDateTime = LocalDateTime.parse(startDateStr);
                }
                if (!jsonObject.isNull("endDateTime")) {
                    String endDateStr = jsonObject.getString("endDateTime");
                    endDateTime = LocalDateTime.parse(endDateStr);
                }
                if (!jsonObject.isNull("shops")) {
                    JSONArray shopsArray = jsonObject.getJSONArray("shops");
                    List<String> shopList = new ArrayList<>();
                    for (int i = 0; i < shopsArray.length(); i++) {
                        shopList.add(shopsArray.getString(i));
                    }
                    shops = shopList;
                }
            } catch (JSONException e) {
                log.error("Error while parsing parameter as JSON, falling back to default parameters.");
            }
        }
        return new JobParams(startDateTime, endDateTime, shops);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobParams params = resolveJobParams(context);
        LocalDateTime startDateTime = params.startDateTime;
        LocalDateTime endDateTime = params.endDateTime;
        List<String> shops = params.shops;
        if (!endDateTime.isAfter(startDateTime)) {
            throw new RuntimeException("EndDateTime must be strictly greater than StartDateTime !");
        }

        List<PlatformOrder> platformOrders = platformOrderService.fetchUninvoicedOrdersForShops(startDateTime, endDateTime, shops);
        List<String> platformOrderIds = platformOrders.stream().map(PlatformOrder::getPlatformOrderId).collect(Collectors.toList());
        List<List<String>> platformOrderIdLists = Lists.partition(platformOrderIds, 10);

        List<OrderListRequestBody> requests = new ArrayList<>();
        for (List<String> platformOrderIdList : platformOrderIdLists) {
            requests.add(new OrderListRequestBody().setPlatformOrderIds(platformOrderIdList));
        }
        List<Order> mabangOrders = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_NUMBER_OF_THREADS);
        List<CompletableFuture<Boolean>> futures = requests.stream()
                .map(request -> CompletableFuture.supplyAsync(() -> {
                    boolean success = false;
                    try {
                        OrderListRawStream rawStream = new OrderListRawStream(request);
                        OrderListStream stream = new OrderListStream(rawStream);
                        List<Order> orders = stream.all();
                        mabangOrders.addAll(orders);
                        success = !orders.isEmpty();
                    } catch (RuntimeException e) {
                        log.error("Error communicating with MabangAPI", e);
                    }
                    return success;
                }, executor))
                .collect(Collectors.toList());
        List<Boolean> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        long nbSuccesses = results.stream().filter(b -> b).count();
        log.info("{}/{} requests have succeeded.", nbSuccesses, requests.size());
        log.info("{}/{} mabang orders have been retrieved.", mabangOrders.size(), platformOrderIds.size());

        log.info("Constructing order modification requests");
        List<ChangeOrderRequestBody> changeOrderRequests = new ArrayList<>();
        for (Order mabangOrder : mabangOrders) {
            Pair<HashSet<Pair<String, Integer>>, HashSet<Pair<String, Integer>>> currentAndAdequateTubes =
                    findCurrentAndAdequateTubes(mabangOrder.getOrderItems());
            HashSet<Pair<String, Integer>> currentTubes = currentAndAdequateTubes.getLeft();
            HashSet<Pair<String, Integer>> adequateTubes = currentAndAdequateTubes.getRight();
            // Do nothing if current tubes are the adequate tubes
            if (!currentTubes.containsAll(adequateTubes) || !adequateTubes.containsAll(currentTubes)) {
                ChangeOrderRequestBody changeOrderRequestBody = ChangeOrderRequestBody.buildChangeOrderRequestBody(
                        mabangOrder.getPlatformOrderId(), null, currentTubes, adequateTubes, null);
                changeOrderRequests.add(changeOrderRequestBody);
            }
        }
        log.info("{} order modification requests to be sent to MabangAPI", changeOrderRequests.size());

        List<CompletableFuture<Boolean>> changeOrderFutures = changeOrderRequests.stream()
                .map(changeOrderRequestBody -> CompletableFuture.supplyAsync(() -> {
                    boolean success = false;
                    try {
                        ChangeOrderRequest changeOrderRequest = new ChangeOrderRequest(changeOrderRequestBody);
                        ChangeOrderResponse response = changeOrderRequest.send();
                        success = response.success();
                    } catch (RuntimeException e) {
                        log.error("Error communicating with MabangAPI", e);
                    }
                    return success;
                }, executor))
                .collect(Collectors.toList());
        results = changeOrderFutures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        nbSuccesses = results.stream().filter(b -> b).count();
        log.info("{}/{} order modification requests have succeeded.", nbSuccesses, changeOrderRequests.size());
    }

    /**
     * Return a pair of sets of tube(String, Integer)s, on the left current tubes (if any, empty if none), on the right the adequate tubes
     *
     * @param orderItems List of order items
     * @return Set of pairs
     */
    private Pair<HashSet<Pair<String, Integer>>, HashSet<Pair<String, Integer>>> findCurrentAndAdequateTubes(List<OrderItem> orderItems) {
        HashSet<Pair<String, Integer>> currentTubes = new HashSet<>();
        HashSet<Pair<String, Integer>> adequateTubes = new HashSet<>();
        CanvasCounts counts = collectCanvasCounts(orderItems, currentTubes);
        addAdequateTubes(adequateTubes, counts);
        return Pair.of(currentTubes, adequateTubes);
    }

    private static CanvasCounts collectCanvasCounts(List<OrderItem> orderItems,
                                                    HashSet<Pair<String, Integer>> currentTubes) {
        CanvasCounts counts = new CanvasCounts();
        for (OrderItem orderItem : orderItems) {
            if (orderItem.isObsolete()) continue;
            String sku = orderItem.getErpCode();
            int quantity = orderItem.getQuantity();
            if (TUBE_SKUS.contains(sku)) {
                currentTubes.add(Pair.of(sku, quantity));
            } else {
                applyCanvasRules(sku, quantity, counts);
            }
        }
        return counts;
    }

    private static void addAdequateTubes(HashSet<Pair<String, Integer>> adequateTubes, CanvasCounts counts) {
        // Aggregate 36*46, 30*40 and 40*60 canvases, with at most three per tube.
        int canvas40TubeCount = counts.canvasNew36Count + counts.canvas30Count + counts.canvas40Count
                + counts.canvasUV30Count + counts.canvasUV40Count;
        addIfPositive(adequateTubes, TUBE_40_SKU,
                (canvas40TubeCount + MAXIMUM_CANVAS_IN_TUBE - 1) / MAXIMUM_CANVAS_IN_TUBE
                        + counts.triptych40TubeCount);
        // Aggregate 46*66 and 50*70 canvases, with at most three per tube.
        int canvas50TubeCount = counts.canvasNew46Count + counts.canvas50Count + counts.canvasUV50Count;
        addIfPositive(adequateTubes, TUBE_50_SKU,
                (canvas50TubeCount + MAXIMUM_CANVAS_IN_TUBE - 1) / MAXIMUM_CANVAS_IN_TUBE
                        + counts.triptych50TubeCount);
        // Pack each full group of three 56*76 canvases together, then the remaining one or two.
        addIfPositive(adequateTubes, TUBE_60_SKU_TREBLE,
                counts.canvasNew56Count / MAXIMUM_56_CANVAS_IN_TUBE + counts.triptych60TubeCount);
        if (counts.canvasNew56Count % MAXIMUM_56_CANVAS_IN_TUBE > 0) {
            addIfPositive(adequateTubes, TUBE_60_SKU_SINGLE_DOUBLE, 1);
        }
    }

    static class CanvasCounts {
        private int triptych40TubeCount;
        private int triptych50TubeCount;
        private int triptych60TubeCount;
        private int canvas30Count;
        private int canvas40Count;
        private int canvas50Count;
        private int canvasNew36Count;
        private int canvasNew46Count;
        private int canvasNew56Count;
        private int canvasUV30Count;
        private int canvasUV40Count;
        private int canvasUV50Count;

        int getCanvas50Count() {
            return canvas50Count;
        }

        int getCanvasNew36Count() {
            return canvasNew36Count;
        }

        int getCanvasUV50Count() {
            return canvasUV50Count;
        }
    }

    private static void addIfPositive(HashSet<Pair<String, Integer>> adequateTubes, String sku, int quantity) {
        if (quantity > 0) {
            adequateTubes.add(Pair.of(sku, quantity));
        }
    }
}
