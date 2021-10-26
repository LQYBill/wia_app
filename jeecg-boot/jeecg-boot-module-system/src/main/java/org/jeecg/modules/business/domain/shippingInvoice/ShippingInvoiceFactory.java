package org.jeecg.modules.business.domain.shippingInvoice;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.codeGeneration.ShippingInvoiceCodeRule;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.ClientMapper;
import org.jeecg.modules.business.mapper.ExchangeRatesMapper;
import org.jeecg.modules.business.mapper.LogisticChannelPriceMapper;
import org.jeecg.modules.business.mapper.ShopMapper;
import org.jeecg.modules.business.service.CountryService;
import org.jeecg.modules.business.service.IPlatformOrderContentService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.service.ISkuDeclaredValueService;
import org.jeecg.modules.business.vo.SkuWeightDiscountServiceFees;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.hutool.core.date.DateTime.now;

@Slf4j
@Component
public class ShippingInvoiceFactory {

    private final IPlatformOrderService platformOrderService;

    private final ClientMapper clientMapper;

    private final ShopMapper shopMapper;

    private final LogisticChannelPriceMapper logisticChannelPriceMapper;

    private final IPlatformOrderContentService platformOrderContentService;

    private final ISkuDeclaredValueService skuDeclaredValueService;

    private final CountryService countryService;

    private final ExchangeRatesMapper exchangeRatesMapper;

    private final SimpleDateFormat SUBJECT_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final List<String> EU_COUNTRY_LIST = Arrays.asList("Austria", "Belgium", "Bulgaria", "Croatia", "Cyprus",
            "Czech", "Denmark", "Estonia", "Finland", "France", "Germany", "Greece", "Hungary", "Ireland", "Italy",
            "Latvia", "Lithuania", "Luxembourg", "Malta", "Netherlands", "Poland", "Portugal", "Romania", "Slovakia",
            "Slovenia", "Spain", "Sweden");

    private LoadingCache<Pair<String, Date>, BigDecimal> declaredValueCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<Pair<String, Date>, BigDecimal>() {
                        @Override
                        public BigDecimal load(Pair<String, Date> skuIdAndDate) {
                            return skuDeclaredValueService.getDeclaredValueForDate(skuIdAndDate.getLeft(), skuIdAndDate.getRight());
                        }
                    });

    public ShippingInvoiceFactory(IPlatformOrderService platformOrderService,
                                  ClientMapper clientMapper,
                                  ShopMapper shopMapper, LogisticChannelPriceMapper logisticChannelPriceMapper,
                                  IPlatformOrderContentService platformOrderContentService,
                                  ISkuDeclaredValueService skuDeclaredValueService,
                                  CountryService countryService,
                                  ExchangeRatesMapper exchangeRatesMapper) {

        this.platformOrderService = platformOrderService;
        this.clientMapper = clientMapper;
        this.shopMapper = shopMapper;
        this.logisticChannelPriceMapper = logisticChannelPriceMapper;
        this.platformOrderContentService = platformOrderContentService;
        this.skuDeclaredValueService = skuDeclaredValueService;
        this.countryService = countryService;
        this.exchangeRatesMapper = exchangeRatesMapper;
    }

    /**
     * Creates a pre-shipping invoice for a client
     * <p>
     * To generate an invoice, it
     * <ol>
     * <li>Search orders and their contents by IDs</li>
     * <li>Generate a new invoice code</li>
     * <li>Find propre logistic channel price for each order </li>
     * <li>Update prices of orders and their contents</li>
     * <li>Generate a invoice</li>
     * <li>Update invoiced their orders and contents to DB</li>
     * </ol>
     *
     * @param customerId the customer id
     * @param ordersIds  the list of order IDs
     * @return the generated invoice
     * @throws UserException if package used by the invoice can not or find more than 1 logistic
     *                       channel price, this exception will be thrown.
     */
    @Transactional
    public ShippingInvoice createPreShippingInvoice(String customerId, List<String> ordersIds) throws UserException {
        log.info("Creating a invoice with arguments:\n client ID: {}, order IDs: {}]", customerId, ordersIds);
        // find orders and their contents of the invoice
        Map<PlatformOrder, List<PlatformOrderContent>> uninvoicedOrderToContent = platformOrderService.fetchOrderData(ordersIds);
        Set<PlatformOrder> platformOrders = uninvoicedOrderToContent.keySet();
        List<String> shopIds = platformOrders.stream()
                .map(PlatformOrder::getShopId)
                .distinct()
                .collect(Collectors.toList());
        log.info("Orders to be invoiced: {}", uninvoicedOrderToContent);
        return createInvoice(customerId, shopIds, uninvoicedOrderToContent, "Pre-Shipping fees");
    }

    private void calculateAndUpdateContentFees(Map<String, BigDecimal> skuRealWeights, Map<String, BigDecimal> skuServiceFees, PlatformOrder uninvoicedOrder, BigDecimal contentWeight, BigDecimal totalShippingFee, BigDecimal clientVatPercentage, Map<PlatformOrderContent, BigDecimal> contentDeclaredValueMap, BigDecimal totalDeclaredValue, BigDecimal totalVAT, boolean vatApplicable, PlatformOrderContent content) {
        String skuId = content.getSkuId();
        BigDecimal realWeight = skuRealWeights.get(skuId);
        // Each content will share the total shipping fee proportionally, because minimum price and unit price
        // vary with total weight, so calculating each content's fee with its weight is just wrong
        content.setShippingFee(realWeight.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                totalShippingFee.multiply(BigDecimal.valueOf(content.getQuantity()))
                        .multiply(realWeight)
                        .divide(contentWeight, RoundingMode.UP)
                        .setScale(2, RoundingMode.UP)
        );
        content.setServiceFee(skuServiceFees.get(skuId)
                .multiply(BigDecimal.valueOf(content.getQuantity()))
                .setScale(2, RoundingMode.UP)
        );
        BigDecimal vat = BigDecimal.ZERO;
        if (vatApplicable) {
            BigDecimal contentDeclaredValue = contentDeclaredValueMap.get(content);
            // Total VAT greater than 0 means total declared value is less than minimum declared value
            // VAT of content must be re-adjusted proportionally
            if (totalVAT.compareTo(BigDecimal.ZERO) > 0) {
                log.info("VAT re-adjusted for SKU : {} of order {}", content.getSkuId(), uninvoicedOrder.getId());
                vat = totalVAT.multiply(contentDeclaredValue)
                        .divide(totalDeclaredValue, RoundingMode.UP)
                        .setScale(2, RoundingMode.UP);
            } else {
                vat = contentDeclaredValue
                        .multiply(clientVatPercentage)
                        .setScale(2, RoundingMode.UP);
            }
        }
        content.setVat(vat);
    }

    /**
     * Creates an invoice based for a client, a list of shops, a date range.
     * <p>
     * To generate an invoice, it
     * <ol>
     * <li>Search orders and their contents based on shop and date range</li>
     * <li>Generate a new invoice code</li>
     * <li>Find propre logistic channel price for each order </li>
     * <li>Update prices of orders and their contents</li>
     * <li>Generate a invoice</li>
     * <li>Update invoiced their orders and contents to DB</li>
     * </ol>
     *
     * @param customerId the customer id
     * @param shopIds    the list of shop codes
     * @param begin      the beginning of the date range
     * @param end        the end of the date range
     * @return the generated invoice
     * @throws UserException if package used by the invoice can not or find more than 1 logistic
     *                       channel price, this exception will be thrown.
     */
    @Transactional
    public ShippingInvoice createInvoice(String customerId, List<String> shopIds, Date begin, Date end) throws UserException {
        log.info(
                "Creating a invoice with arguments:\n client ID: {}, shop IDs: {}, period:[{} - {}]",
                customerId, shopIds.toString(), begin, end
        );
        // find orders and their contents of the invoice
        Map<PlatformOrder, List<PlatformOrderContent>> uninvoicedOrderToContent = platformOrderService.findUninvoicedOrders(shopIds, begin, end);
        String subject = String.format(
                "Shipping fees from %s to %s",
                SUBJECT_FORMAT.format(begin),
                SUBJECT_FORMAT.format(end)
        );
        return createInvoice(customerId, shopIds, uninvoicedOrderToContent, subject);
    }

    /**
     * Creates an invoice based for a client, a list of shops, a date range.
     * <p>
     * To generate an invoice, it
     * <ol>
     * <li>Search orders and their contents based on shop and date range</li>
     * <li>Generate a new invoice code</li>
     * <li>Find propre logistic channel price for each order </li>
     * <li>Update prices of orders and their contents</li>
     * <li>Generate a invoice</li>
     * <li>Update invoiced their orders and contents to DB</li>
     * </ol>
     *
     * @param customerId Customer ID
     * @param shopIds Shop IDs
     * @param subject Invoice subject
     * @return the generated invoice
     * @throws UserException if package used by the invoice can not or find more than 1 logistic
     *                       channel price, this exception will be thrown.
     */
    @Transactional
    public ShippingInvoice createInvoice(String customerId, List<String> shopIds,
                                         Map<PlatformOrder, List<PlatformOrderContent>> orderAndContent,
                                         String subject) throws UserException {
        log.info("Orders to be invoiced: {}", orderAndContent);
        if (orderAndContent == null) {
            throw new UserException("None platform order in the selected period!");
        }
        Map<String, BigDecimal> skuRealWeights = new HashMap<>();
        Map<String, BigDecimal> skuServiceFees = new HashMap<>();
        for (SkuWeightDiscountServiceFees skuWeightDiscountServiceFees : platformOrderContentService.getAllSKUWeightsDiscountsServiceFees()) {
            if (skuWeightDiscountServiceFees.getWeight() != null) {
                skuRealWeights.put(skuWeightDiscountServiceFees.getSkuId(),
                        skuWeightDiscountServiceFees.getDiscount().multiply(BigDecimal.valueOf(skuWeightDiscountServiceFees.getWeight())));
            }
            skuServiceFees.put(skuWeightDiscountServiceFees.getSkuId(), skuWeightDiscountServiceFees.getServiceFees());
        }

        Client client = clientMapper.selectById(customerId);
        List<Shop> shops = shopMapper.selectBatchIds(shopIds);
        Map<String, BigDecimal> shopServiceFeeMap = new HashMap<>();
        shops.forEach(shop -> shopServiceFeeMap.put(shop.getId(), shop.getOrderServiceFee()));
        String invoiceCode = generateInvoiceCode();
        log.info("New invoice code: {}", invoiceCode);
        // find logistic channel price for each order based on its content
        for (PlatformOrder uninvoicedOrder : orderAndContent.keySet()) {
            List<PlatformOrderContent> contents = orderAndContent.get(uninvoicedOrder);
            if (contents.size() == 0) {
                throw new UserException("Order: {} doesn't have content", uninvoicedOrder.getPlatformOrderId());
            }
            log.info("Searching price for {} of order {}", contents, uninvoicedOrder);
            Map<String, Integer> contentMap = new HashMap<>();
            for (PlatformOrderContent content : contents) {
                contentMap.put(content.getSkuId(), content.getQuantity());
            }

            // calculate weight of an order
            BigDecimal contentWeight = platformOrderContentService.calculateWeight(
                    uninvoicedOrder.getLogisticChannelName(),
                    contentMap,
                    skuRealWeights
            );
            /* Convert country name to country name */

            LogisticChannelPrice price = findAppropriatePrice(uninvoicedOrder, contentWeight);
            // update attributes of orders and theirs content
            uninvoicedOrder.setFretFee(price.getRegistrationFee());
            uninvoicedOrder.setOrderServiceFee(shopServiceFeeMap.get(uninvoicedOrder.getShopId()));
            uninvoicedOrder.setShippingInvoiceNumber(invoiceCode);
            BigDecimal totalShippingFee = price.calculateShippingPrice(contentWeight);
            BigDecimal clientVatPercentage = client.getVatPercentage();
            Map<PlatformOrderContent, BigDecimal> contentDeclaredValueMap = new HashMap<>();
            BigDecimal totalDeclaredValue = calculateTotalDeclaredValue(uninvoicedOrder, contents, contentDeclaredValueMap);
            BigDecimal totalVAT = BigDecimal.ZERO;
            boolean vatApplicable = clientVatPercentage.compareTo(BigDecimal.ZERO) > 0
                    && EU_COUNTRY_LIST.contains(uninvoicedOrder.getCountry());
            // In case where VAT is applicable, and the transport line has a minimum declared value (MDV) per PACKAGE
            // We need to first calculate the total declared value and compare it to the MDV
            // If the total declared value is below the MDV, then the VAT should be calculated with the MDV and
            // then proportionally applied to each content
            BigDecimal minimumDeclaredValue = price.getMinimumDeclaredValue();
            if (vatApplicable && minimumDeclaredValue != null) {
                totalVAT = calculateTotalVat(totalDeclaredValue, clientVatPercentage, minimumDeclaredValue);
            }
            for (PlatformOrderContent content : contents) {
                calculateAndUpdateContentFees(skuRealWeights, skuServiceFees, uninvoicedOrder, contentWeight,
                        totalShippingFee, clientVatPercentage, contentDeclaredValueMap, totalDeclaredValue, totalVAT,
                        vatApplicable, content);
            }
        }
        BigDecimal eurToUsd = exchangeRatesMapper.getLatestExchangeRate("EUR", "USD");
        ShippingInvoice invoice = new ShippingInvoice(client, invoiceCode, subject, orderAndContent, eurToUsd);
        // update them to DB after invoiced
        platformOrderService.updateBatchById(orderAndContent.keySet());
        platformOrderContentService.updateBatchById(orderAndContent.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return invoice;
    }

    private BigDecimal calculateTotalVat(BigDecimal totalDeclaredValue, BigDecimal clientVatPercentage,
                                         BigDecimal minimumDeclaredValue) {
        BigDecimal totalVAT = BigDecimal.ZERO;
        if (totalDeclaredValue.compareTo(minimumDeclaredValue) < 0) {
            totalVAT = minimumDeclaredValue.multiply(clientVatPercentage)
                    .setScale(2, RoundingMode.UP);
        }
        return totalVAT;
    }

    private BigDecimal calculateTotalDeclaredValue(PlatformOrder order, List<PlatformOrderContent> contents,
                                                   Map<PlatformOrderContent, BigDecimal> contentDeclaredValueMap) throws UserException {
        BigDecimal totalDeclaredValue = BigDecimal.ZERO;
        Date shippingTime = order.getShippingTime() == null ? now().toSqlDate() : order.getShippingTime();
        for (PlatformOrderContent content : contents) {
            String skuId = content.getSkuId();
            BigDecimal declaredValueForSKU;
            try {
                declaredValueForSKU = declaredValueCache.get(Pair.of(skuId, shippingTime));
            } catch (ExecutionException e) {
                String msg = "Error while retrieving declared value of SKU " + skuId + " of order "
                        + content.getPlatformOrderId();
                log.error(e.getMessage());
                throw new UserException(msg);
            }
            BigDecimal contentDeclaredValue = declaredValueForSKU.multiply(BigDecimal.valueOf(content.getQuantity()));
            contentDeclaredValueMap.put(content, contentDeclaredValue);
            totalDeclaredValue = totalDeclaredValue.add(contentDeclaredValue);
        }
        return totalDeclaredValue;
    }

    @NotNull
    private LogisticChannelPrice findAppropriatePrice(PlatformOrder uninvoicedOrder, BigDecimal contentWeight) throws UserException {
        LogisticChannelPrice price;
        try {
            /* Find channel price */
            Country country = countryService.findByEnName(uninvoicedOrder.getCountry());

            Date shippingTime = uninvoicedOrder.getShippingTime() == null ? now().toSqlDate() : uninvoicedOrder.getShippingTime();
            price = logisticChannelPriceMapper.findBy(
                    uninvoicedOrder.getLogisticChannelName(),
                    // For orders without shipping time (pre-shipping), use today
                    shippingTime,
                    contentWeight,
                    country.getCode()
            );
            if (price == null) {
                String format = "Can not find propre channel price for" +
                        "package Serial No: %s, delivered at %s, " +
                        "weight: %s, channel name: %s, destination: %s";
                String msg = String.format(
                        format,
                        uninvoicedOrder.getPlatformOrderId(),
                        shippingTime,
                        contentWeight,
                        uninvoicedOrder.getLogisticChannelName(),
                        uninvoicedOrder.getCountry()
                );
                log.error(msg);
                throw new UserException(msg);
            }
        } catch (IncorrectResultSizeDataAccessException e) {
            String msg = "Found more than 1 channel price for package Order No: " + uninvoicedOrder.getPlatformOrderNumber()
                    + ", delivered at " + uninvoicedOrder.getShippingTime().toString();
            log.error(msg);
            throw new UserException(msg);
        }
        return price;
    }

    /**
     * Generate a new invoice code, it is generated based on latest invoice's code.
     * <p>
     * If there is no invoice this month, the new code will be N°yyyy-MM-2001,
     * otherwise, the new code will be N°yyyy-MM-No, where "No" is the "No" part of last invoice's code + 1.
     *
     * @return the invoice code.
     */
    private String generateInvoiceCode() {
        String lastInvoiceCode = platformOrderService.findPreviousInvoice();

        ShippingInvoiceCodeRule rule = new ShippingInvoiceCodeRule();
        return rule.next(lastInvoiceCode);
    }
}
