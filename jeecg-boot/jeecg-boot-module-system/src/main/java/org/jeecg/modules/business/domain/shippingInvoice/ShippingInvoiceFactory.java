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
import org.jeecg.modules.business.service.CountryService;
import org.jeecg.modules.business.service.IPlatformOrderContentService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.service.ISkuDeclaredValueService;
import org.jeecg.modules.business.vo.SkuWeightDiscount;
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

@Slf4j
@Component
public class ShippingInvoiceFactory {

    private final IPlatformOrderService platformOrderService;

    private final ClientMapper clientMapper;

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
                                  LogisticChannelPriceMapper logisticChannelPriceMapper,
                                  IPlatformOrderContentService platformOrderContentService,
                                  ISkuDeclaredValueService skuDeclaredValueService,
                                  CountryService countryService,
                                  ExchangeRatesMapper exchangeRatesMapper) {

        this.platformOrderService = platformOrderService;
        this.clientMapper = clientMapper;
        this.logisticChannelPriceMapper = logisticChannelPriceMapper;
        this.platformOrderContentService = platformOrderContentService;
        this.skuDeclaredValueService = skuDeclaredValueService;
        this.countryService = countryService;
        this.exchangeRatesMapper = exchangeRatesMapper;
    }

    /**
     * Creates a invoice based for a client, a list of shops, a date range.
     * <p>
     * To generate a invoice, it
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
        log.info("Orders to be invoiced: {}", uninvoicedOrderToContent);
        if (uninvoicedOrderToContent == null) {
            throw new UserException("None platform order in the selected period!");
        }
        Map<String, BigDecimal> skuRealWeights = new HashMap<>();
        for (SkuWeightDiscount skuWeightsAndDiscount : platformOrderContentService.getAllSKUWeightsAndDiscounts()) {
            if (skuWeightsAndDiscount.getWeight() != null) {
                skuRealWeights.put(skuWeightsAndDiscount.getSkuId(),
                        skuWeightsAndDiscount.getDiscount().multiply(BigDecimal.valueOf(skuWeightsAndDiscount.getWeight())));
            }
        }

        Client client = clientMapper.selectById(customerId);
        String invoiceCode = generateInvoiceCode();
        log.info("New invoice code: {}", invoiceCode);
        // find logistic channel price for each order based on its content
        for (PlatformOrder uninvoicedOrder : uninvoicedOrderToContent.keySet()) {
            List<PlatformOrderContent> contents = uninvoicedOrderToContent.get(uninvoicedOrder);
            if (contents.size() == 0) {
                throw new UserException("Order: {} doesn't have content", uninvoicedOrder.getPlatformOrderId());
            }
            log.info("Searching price for {} of order {}", contents, uninvoicedOrder);
            Map<String, Integer> contentMap = new HashMap<>();
            for (PlatformOrderContent content : contents) {
                contentMap.put(content.getSkuId(), content.getQuantity());
            }

            LogisticChannelPrice price;
            // calculate weight of a order
            BigDecimal contentWeight = platformOrderContentService.calculateWeight(
                    uninvoicedOrder.getLogisticChannelName(),
                    contentMap,
                    skuRealWeights
            );
            /* Convert country name to country name */

            try {
                /* Find channel price */
                Country country = countryService.findByEnName(uninvoicedOrder.getCountry());

                price = logisticChannelPriceMapper.findBy(
                        uninvoicedOrder.getLogisticChannelName(),
                        uninvoicedOrder.getShippingTime(),
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
                            uninvoicedOrder.getShippingTime(),
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
            // update attributes of orders and theirs content
            uninvoicedOrder.setFretFee(price.getRegistrationFee());
            uninvoicedOrder.setShippingInvoiceNumber(invoiceCode);
            BigDecimal totalShippingFee = price.calculateShippingPrice(contentWeight);
            for (PlatformOrderContent content : contents) {
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
                content.setServiceFee(price.getAdditionalCost());
                BigDecimal clientVatPercentage = client.getVatPercentage();
                BigDecimal vat = BigDecimal.ZERO;
                if (clientVatPercentage.compareTo(BigDecimal.ZERO) > 0 && EU_COUNTRY_LIST.contains(uninvoicedOrder.getCountry())) {
                    try {
                        vat = declaredValueCache.get(Pair.of(skuId, uninvoicedOrder.getShippingTime()))
                                .multiply(BigDecimal.valueOf(content.getQuantity()))
                                .multiply(clientVatPercentage)
                                .setScale(2, RoundingMode.UP);
                    } catch (ExecutionException e) {
                        String msg = "Error while retrieving declared value of SKU " + skuId + " of order "
                                + content.getPlatformOrderId();
                        log.error(e.getMessage());
                        throw new UserException(msg);
                    }
                }
                content.setVat(vat);
            }
        }

        String subject = String.format(
                "Shipping fees from %s to %s",
                SUBJECT_FORMAT.format(begin),
                SUBJECT_FORMAT.format(end)
        );
        BigDecimal eurToUsd = exchangeRatesMapper.getLatestExchangeRate("EUR", "USD");
        ShippingInvoice invoice = new ShippingInvoice(client, invoiceCode, subject, uninvoicedOrderToContent, eurToUsd);
        // update them to DB after invoiced
        platformOrderService.updateBatchById(uninvoicedOrderToContent.keySet());
        platformOrderContentService.updateBatchById(uninvoicedOrderToContent.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return invoice;
    }

    /**
     * Generate a new invoice code, it is generated based on latest invoice's code.
     * <p>
     * If there is no invoice this month, the new code will be N°yyyy-MM-2001,
     * otherwise, the new code will be N°yyyy-MM-No, where "No" is the "No" part of latest invoice's code + 1.
     *
     * @return the invoice code.
     */
    private String generateInvoiceCode() {
        String lastInvoiceCode = platformOrderService.findPreviousInvoice();

        ShippingInvoiceCodeRule rule = new ShippingInvoiceCodeRule();
        return rule.next(lastInvoiceCode);
    }
}
