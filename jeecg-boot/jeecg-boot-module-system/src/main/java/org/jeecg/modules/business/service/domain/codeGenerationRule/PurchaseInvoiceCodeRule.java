package org.jeecg.modules.business.service.domain.codeGenerationRule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PurchaseInvoiceCodeRule implements CodeGenerationRule<String> {

    @Override
    public String next(String previous) {
        String newInvoiceCode;
        if (previous != null) {
            String[] codeParts = previous.split("-");
            codeParts[2] = "" + (Integer.parseInt(codeParts[2]) + 1);
            newInvoiceCode = String.join("-", codeParts);
        } else {
            String yearAndMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            newInvoiceCode = "" + yearAndMonth + "-1001";
        }
        return newInvoiceCode;
    }
}
