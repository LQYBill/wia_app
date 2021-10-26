package org.jeecg.modules.business.domain.shippingInvoice;

import org.jeecg.modules.business.domain.invoice.AbstractInvoice;
import org.jeecg.modules.business.domain.invoice.InvoiceStyleFactory;
import org.jeecg.modules.business.domain.invoice.Row;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.PlatformOrder;
import org.jeecg.modules.business.entity.PlatformOrderContent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class represent the invoice file needed in business process, since the generation of this file
 * need complex data, instance of class can only be created by its factory.
 */
public class ShippingInvoice extends AbstractInvoice<String, Object, Integer, Object, BigDecimal> {
    private final Map<PlatformOrder, List<PlatformOrderContent>> ordersToContent;

    private final BigDecimal exchangeRate;

    private BigDecimal totalAmount;

    private final static String DOLLAR_LOCATION = "H43";

    public ShippingInvoice(Client targetClient, String code,
                           String subject,
                           Map<PlatformOrder, List<PlatformOrderContent>> ordersToContent,
                           BigDecimal exchangeRate) {
        super(targetClient, code, subject);
        this.ordersToContent = ordersToContent;
        this.exchangeRate = exchangeRate;
        totalAmount = BigDecimal.ZERO;
    }

    /**
     * Generates row content based on package data, content of row is determined by business process.
     *
     * @return a list of generated row
     */
    @Override
    protected List<Row<String, Object, Integer, Object, BigDecimal>> tableData() {
        Map<String, List<PlatformOrder>> countryPackageMap = ordersToContent.keySet().stream()
                .collect(
                        Collectors.groupingBy(PlatformOrder::getCountry)
                );
        Map<String, List<PlatformOrderContent>> orderMap = ordersToContent.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getPlatformOrderId(), Map.Entry::getValue));

        List<Row<String, Object, Integer, Object, BigDecimal>> rows = new ArrayList<>();

        BigDecimal vatForEU = BigDecimal.ZERO;
        BigDecimal totalServiceFees = BigDecimal.ZERO;
        for (Map.Entry<String, List<PlatformOrder>> entry : countryPackageMap.entrySet()) {
            String country = entry.getKey();
            List<PlatformOrder> orders = entry.getValue();

            BigDecimal countryShippingFees = BigDecimal.ZERO;
            BigDecimal countryFretFees = BigDecimal.ZERO;
            BigDecimal countryServiceFeesPerOrder = BigDecimal.ZERO;
            BigDecimal countryServiceFeesPerSKU = BigDecimal.ZERO;
            for (PlatformOrder po : orders) {
                countryFretFees = countryFretFees.add(po.getFretFee());
                countryServiceFeesPerOrder = countryServiceFeesPerOrder.add(po.getOrderServiceFee());
                for (PlatformOrderContent poc : orderMap.get(po.getPlatformOrderId())) {
                    countryShippingFees = countryShippingFees.add(poc.getShippingFee());
                    vatForEU = vatForEU.add(poc.getVat());
                    countryServiceFeesPerSKU = countryServiceFeesPerSKU.add(poc.getServiceFee());
                }
            }
            totalServiceFees = totalServiceFees.add(countryServiceFeesPerOrder).add(countryServiceFeesPerSKU);
            Row<String, Object, Integer, Object, BigDecimal> row = new Row<>(
                    String.format("Total shipping cost for %s", country),
                    null,
                    orders.size(),
                    null,
                    countryShippingFees.add(countryFretFees)
            );
            rows.add(row);
            totalAmount = totalAmount
                    .add(countryShippingFees)
                    .add(countryFretFees)
                    .add(countryServiceFeesPerOrder)
                    .add(countryServiceFeesPerSKU);
        }
        Row<String, Object, Integer, Object, BigDecimal> vatRow = new Row<>(
                "Total VAT fee for EU",
                null,
                null,
                null,
                vatForEU
        );
        rows.add(vatRow);
        Row<String, Object, Integer, Object, BigDecimal> serviceFeeRow = new Row<>(
                "Total service fee",
                null,
                null,
                null,
                totalServiceFees
        );
        rows.add(serviceFeeRow);
        totalAmount = totalAmount.add(vatForEU);
        return rows;
    }


    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public BigDecimal reducedAmount() {
        return BigDecimal.ZERO;
    }

    public BigDecimal paidAmount() {
        // function not implemented yet, temporarily zero
        return BigDecimal.ZERO;
    }


    /**
     * In addition to super's operation, if target client prefer dollar, write the formula of exchange to
     * a specific cell.
     *
     * @param factory factory that provide style
     */
    @Override
    protected void fillTable(InvoiceStyleFactory factory) {
        super.fillTable(factory);
        if (targetClient.getCurrency().equals("USD")) {
            String formula = "H42 *" + exchangeRate;
            writer.getCell(DOLLAR_LOCATION).setCellFormula(formula);
        }
    }
}
