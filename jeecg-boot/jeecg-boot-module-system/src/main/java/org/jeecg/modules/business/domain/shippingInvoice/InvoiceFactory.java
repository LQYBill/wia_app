package org.jeecg.modules.business.domain.shippingInvoice;

import com.galaxystudio.kpproject.modules.base.entity.DBCustomer;
import com.galaxystudio.kpproject.modules.base.entity.DBLogisticsChannelPrice;
import com.galaxystudio.kpproject.modules.base.exception.UserException;
import com.galaxystudio.kpproject.modules.base.repository.*;
import com.galaxystudio.kpproject.modules.base.repository.PackageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class InvoiceFactory {

    private final InvoiceRepository invoiceRepository;

    private final PackageRepository packageRepository;

    private final CustomerRepository customerRepository;

    private final LogisticsChannelPriceRepository logisticsChannelPriceRepository;

    public InvoiceFactory(InvoiceRepository invoiceRepository, PackageRepository packageRepository, CustomerRepository customerRepository, LogisticsChannelPriceRepository logisticsChannelPriceRepository) {
        this.invoiceRepository = invoiceRepository;
        this.packageRepository = packageRepository;
        this.customerRepository = customerRepository;
        this.logisticsChannelPriceRepository = logisticsChannelPriceRepository;
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
    public Invoice createInvoice(String customerId, List<String> shopIds, Date begin, Date end) throws UserException {
        log.info(
                "Creating a Invoice with arguments:\n customer ID: {}, shop IDs: {}, period:[{} - {}]",
                customerId, shopIds.toString(), begin, end
        );
        List<Package> packages = packageRepository.getUninvoicedPackage(shopIds, begin, end);
        if(packages == null){
            throw new UserException("No packages in the selected period!");
        }
        for (Package p : packages) {
            try {
                DBLogisticsChannelPrice price = logisticsChannelPriceRepository.findLogisticsChannelPrice(
                        p.getLogisticsChannel().getName(),
                        p.getDeliverTime(),
                        p.getWeight(),
                        p.getCountry().getCode()
                );
                if (price == null) {
                    String msg = "Can not find propre channel price for package Serial No: "
                            + p.getOrderSerial() + ", delivered at " + p.getDeliverTime().toString();
                    log.error(msg);
                    throw new UserException(msg);
                }
                p.updateFees(price);
            } catch (IncorrectResultSizeDataAccessException e) {
                String msg = "Found more than 1 channel price for package Order No: " + p.getOrderNo()
                        + ", delivered at " + p.getDeliverTime().toString();
                log.error(msg);
                throw new UserException(msg);
            }
        }
        String invoiceCode = generateInvoiceCode();
        DBCustomer customer = customerRepository.getById(customerId);
        return new com.galaxystudio.kpproject.modules.base.domain.invoice.Invoice(invoiceCode, customer, begin, end, packages, 1.0);
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
        String newInvoiceCode;
        // concatenate the new invoice code
        String lastInvoiceCode = this.invoiceRepository.getPreviousInvoice();
        if (lastInvoiceCode != null) {
            String[] codeParts = lastInvoiceCode.split("-");
            codeParts[2] = "" + (Integer.parseInt(codeParts[2]) + 1);
            newInvoiceCode = String.join("-", codeParts);
        } else {
            String yearAndMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            newInvoiceCode = "N°" + yearAndMonth + "-2001";
        }
        return newInvoiceCode;
    }
}
