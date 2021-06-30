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
 * need complex data, instance of class can only be created by it's factory.
 */
public class ShippingInvoice extends AbstractInvoice<String, Object, Integer, Object, String> {
    private final Map<PlatformOrder, List<PlatformOrderContent>> ordersToContent;

    private final BigDecimal exchangeRate;

    private final static String DOLLAR_LOCATION = "H43";

    public ShippingInvoice(Client targetClient, String code,
                           String subject,
                           Map<PlatformOrder, List<PlatformOrderContent>> ordersToContent,
                           BigDecimal exchangeRate) {
        super(targetClient, code, subject);
        this.ordersToContent = ordersToContent;
        this.exchangeRate = exchangeRate;
    }

    /**
     * Generates row content based on package data, content of row is determined by business process.
     *
     * @return a list of generated row
     */
    @Override
    protected List<Row<String, Object, Integer, Object, String>> tableData() {
        Map<String, List<PlatformOrder>> countryPackageMap = ordersToContent.keySet().stream()
                .collect(
                        Collectors.groupingBy(PlatformOrder::getCountry)
                );
        Map<String, List<PlatformOrderContent>> orderMap = ordersToContent.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getPlatformOrderId(), Map.Entry::getValue));

        List<Row<String, Object, Integer, Object, String>> rows = new ArrayList<>();

        for (Map.Entry<String, List<PlatformOrder>> entry : countryPackageMap.entrySet()) {
            String country = entry.getKey();
            List<PlatformOrder> orders = entry.getValue();

            BigDecimal countryOtherFees = orders.stream()
                    .map(o ->
                            orderMap.get(o.getPlatformOrderId()).stream()
                                    .map(PlatformOrderContent::getTotalFee)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    )
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal countryFretFees = orders.stream()
                    .map(PlatformOrder::getFretFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Row<String, Object, Integer, Object, String> row = new Row<>(
                    String.format("Total cost for %s", country),
                    null,
                    orders.size(),
                    null,
                    String.format("%.2f", countryOtherFees.add(countryFretFees))
            );
            rows.add(row);
        }
        return rows;
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
            String formula = "=H42*" + exchangeRate.toString();
            writer.getCell(DOLLAR_LOCATION).setCellFormula(formula);
        }
    }
}
