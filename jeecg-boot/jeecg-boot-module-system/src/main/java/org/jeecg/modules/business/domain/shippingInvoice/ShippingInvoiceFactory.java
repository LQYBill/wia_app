package org.jeecg.modules.business.domain.shippingInvoice;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.codeGeneration.ShippingInvoiceCodeRule;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.ClientMapper;
import org.jeecg.modules.business.mapper.LogisticChannelPriceMapper;
import org.jeecg.modules.business.service.CountryService;
import org.jeecg.modules.business.service.IPlatformOrderContentService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.jeecg.modules.business.service.exception.MultipleMatchException;
import org.jeecg.modules.business.service.exception.ZeroResultException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ShippingInvoiceFactory {

    private final IPlatformOrderService platformOrderService;

    private final ClientMapper clientMapper;

    private final LogisticChannelPriceMapper logisticChannelPriceMapper;

    private final IPlatformOrderContentService platformOrderContentService;

    private final CountryService countryService;

    private final SimpleDateFormat SUBJECT_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public ShippingInvoiceFactory(IPlatformOrderService platformOrderService,
                                  ClientMapper clientMapper,
                                  LogisticChannelPriceMapper logisticChannelPriceMapper,
                                  IPlatformOrderContentService platformOrderContentService, CountryService countryService) {

        this.platformOrderService = platformOrderService;
        this.clientMapper = clientMapper;
        this.logisticChannelPriceMapper = logisticChannelPriceMapper;
        this.platformOrderContentService = platformOrderContentService;
        this.countryService = countryService;
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
        if (uninvoicedOrderToContent == null) {
            throw new UserException("None platform order in the selected period!");
        }

        String invoiceCode = generateInvoiceCode();
        // find logistic channel price for each order based on its content
        for (PlatformOrder uninvoicedOrder : uninvoicedOrderToContent.keySet()) {
            List<PlatformOrderContent> contents = uninvoicedOrderToContent.get(uninvoicedOrder);
            LogisticChannelPrice price;
            // calculate weight of a order
            BigDecimal contentWeight = platformOrderContentService.calculateWeight(
                    uninvoicedOrder.getLogisticChannelName(),
                    contents
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
            contents.forEach(content -> {
                content.setShippingFee(price.calculateShippingPrice(contentWeight));
                content.setServiceFee(price.getAdditionalCost());
            });
        }
        Client client = clientMapper.selectById(customerId);
        String subject = String.format(
                "Shipping fees from %s to %s",
                SUBJECT_FORMAT.format(begin),
                SUBJECT_FORMAT.format(end)
        );
        ShippingInvoice invoice = new ShippingInvoice(client, invoiceCode, subject, uninvoicedOrderToContent, BigDecimal.valueOf(1));
        // update them to DB after invoiced
        platformOrderService.updatePlatformOrder(uninvoicedOrderToContent);
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
