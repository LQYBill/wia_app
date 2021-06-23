package org.jeecg.modules.business.domain.shippingInvoice;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.codeGeneration.ShippingInvoiceCodeRule;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.LogisticChannelPrice;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;
import org.jeecg.modules.business.mapper.ClientMapper;
import org.jeecg.modules.business.mapper.LogisticChannelPriceMapper;
import org.jeecg.modules.business.service.IPlatformOrderContentService;
import org.jeecg.modules.business.service.IPlatformOrderService;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    public ShippingInvoiceFactory(IPlatformOrderService platformOrderService,
                                  ClientMapper clientMapper,
                                  LogisticChannelPriceMapper logisticChannelPriceMapper,
                                  IPlatformOrderContentService platformOrderContentService) {

        this.platformOrderService = platformOrderService;
        this.clientMapper = clientMapper;
        this.logisticChannelPriceMapper = logisticChannelPriceMapper;
        this.platformOrderContentService = platformOrderContentService;
    }

    /**
     * Creates a invoice based on a customer id, a list of shop codes, a date range.
     * <p>
     * To generate a invoice, it
     * <ol>
     * <li>generate a new invoice code</li>
     * <li>selects all uninvoiced packages from repository</li>
     * <li>update package's logistics cost</li>
     * <li>gives them to the invoice file</li>
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
    public ShippingInvoice createInvoice(String customerId, List<String> shopIds, Date begin, Date end) throws UserException {
        log.info(
                "Creating a Invoice with arguments:\n customer ID: {}, shop IDs: {}, period:[{} - {}]",
                customerId, shopIds.toString(), begin, end
        );
        // find orders and contents of the invoice
        Map<PlatformOrder, List<PlatformOrderContent>> uninvoicedOrderToContent = platformOrderService.findUninvoicedOrders(shopIds, begin, end);
        if (uninvoicedOrderToContent == null) {
            throw new UserException("No packages in the selected period!");
        }

        String invoiceCode = generateInvoiceCode();
        // find logistic channel price for each order based on its content
        for (PlatformOrder uninvoicedOrder : uninvoicedOrderToContent.keySet()) {
            List<PlatformOrderContent> contents = uninvoicedOrderToContent.get(uninvoicedOrder);
            LogisticChannelPrice price;
            BigDecimal contentWeight = platformOrderContentService.calculateWeight(
                    uninvoicedOrder.getLogisticChannelName(),
                    contents
            );
            try {
                price = logisticChannelPriceMapper.findBy(
                        uninvoicedOrder.getLogisticChannelName(),
                        uninvoicedOrder.getShippingTime(),
                        contentWeight,
                        uninvoicedOrder.getCountry()
                );
                if (price == null) {
                    String msg = String.format(
                            "Can not find propre channel price for" +
                                    "package Serial No: %s," +
                                    " delivered at %s, " +
                                    "weight: %s, " +
                                    "channel name: %s, " +
                                    "destination: %s",
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
            uninvoicedOrder.setFretFee(price.getRegistrationFee());
            uninvoicedOrder.setShippingInvoiceNumber(invoiceCode);
            contents.forEach(content -> {
                content.setShippingFee(price.calculateShippingPrice(contentWeight));
                content.setServiceFee(price.getAdditionalCost());
            });
        }
        platformOrderService.updatePlatformOrder(uninvoicedOrderToContent);
        Client customer = clientMapper.selectById(customerId);
        String subject = String.format("Shipping fees from %s to %s", begin, end);

        return new ShippingInvoice(customer, invoiceCode, subject, uninvoicedOrderToContent, BigDecimal.valueOf(1));
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

    /**
     * Update data to DB after be invoiced
     *
     * @param invoicedOrderToContent invoiced data to update in DB
     */
    private void updateAfterInvoiced(Map<PlatformOrder, List<PlatformOrderContent>> invoicedOrderToContent) {

    }
}
