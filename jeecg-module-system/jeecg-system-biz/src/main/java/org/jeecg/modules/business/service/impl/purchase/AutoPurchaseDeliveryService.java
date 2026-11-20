package org.jeecg.modules.business.service.impl.purchase;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.business.entity.AutoPurchaseRun;
import org.jeecg.modules.business.entity.PurchaseOrder;
import org.jeecg.modules.business.mapper.AutoPurchaseMapper;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import javax.mail.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class AutoPurchaseDeliveryService {
    private final AutoPurchaseMapper runs;
    private final IPurchaseOrderService purchases;
    private final IProviderMabangService providers;
    private final EmailService emails;
    private final Environment environment;
    private final PurchaseDetailExportService details;
    @Value("${jeecg.path.purchaseInvoiceDir}") private String invoiceDir;

    public AutoPurchaseDeliveryService(AutoPurchaseMapper runs, IPurchaseOrderService purchases,
            IProviderMabangService providers, EmailService emails, Environment environment, PurchaseDetailExportService details) {
        this.runs = runs; this.purchases = purchases; this.providers = providers;
        this.emails = emails; this.environment = environment;
        this.details = details;
    }

    public void deliver(AutoPurchaseRun run) throws Exception {
        if (run == null || "DONE".equals(run.getPlanState())) return;
        if (!"READY".equals(run.getPlanState())) {
            throw new IllegalStateException("Automatic purchase " + run.getId() + " requires reconciliation: " + run.getPlanState());
        }
        // Cross-job-key and multi-node protection; committed before external effects.
        if (runs.claimPlan(run.getId()) != 1) return;
        boolean submitted = false;
        boolean recorded = false;
        try {
            PurchaseOrder order = purchases.getById(run.getPurchaseId());
            if (order == null || !Integer.valueOf(1).equals(order.getStatus()) || order.isOrdered()) {
                throw new IllegalStateException("Invoice cancelled, missing, or already ordered: " + run.getPurchaseId());
            }
            InvoiceMetaData meta = purchases.makeAutomaticInvoice(run.getPurchaseId(), run.getClientId());
            if (!Boolean.TRUE.equals(run.getEmailSent())) {
                sendInvoice(run, meta);
                runs.emailSent(run.getId());
            }
            List<SkuQuantity> items = purchases.getSkuQuantityByInvoiceNumber(meta.getInvoiceCode());
            if (items.isEmpty() || items.stream().anyMatch(item -> item.getQuantity() == null || item.getQuantity() <= 0
                    || item.getErpCode() == null || item.getErpCode().trim().isEmpty())) {
                throw new IllegalStateException("Invalid invoice SKU quantities: " + meta.getInvoiceCode());
            }
            Map<String, Integer> quantities = items.stream().collect(Collectors.toMap(
                    SkuQuantity::getErpCode, SkuQuantity::getQuantity, Math::addExact));
            submitted = true;
            Responses result = providers.addAutomaticPurchaseOrderToMabang(quantities, meta,
                    new AtomicReference<>(new HashMap<>()), run.getMabangUsername());
            boolean success = result != null && result.getFailures().isEmpty() && !result.getSuccesses().isEmpty();
            runs.finishPlan(run.getId(), success ? "DONE" : "UNKNOWN", JSON.toJSONString(result));
            recorded = true;
            if (!success) throw new IllegalStateException("Mabang plan requires reconciliation for " + meta.getInvoiceCode());
        } catch (Exception e) {
            // A timeout or partial success can mean Mabang accepted the request. Never blindly resubmit.
            if (!submitted) runs.finishPlan(run.getId(), "READY", e.toString());
            else if (!recorded) {
                runs.finishPlan(run.getId(), "UNKNOWN", e.toString());
            }
            throw e;
        }
    }

    private void sendInvoice(AutoPurchaseRun run, InvoiceMetaData meta) throws Exception {
        Session session = Session.getInstance(emails.getMailSender(), new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(environment.getProperty("spring.mail.username"), environment.getProperty("spring.mail.password"));
            }
        });
        String detailFile = details.export(run.getPurchaseId(), meta).toString();
        emails.sendMessageWithAttachments(run.getRecipient(), "Automatic purchase invoice " + meta.getInvoiceCode(),
                "Please find your automatic replenishment invoice and purchase details attached. "
                        + "Detail amounts are in EUR before promotions; please refer to the invoice for the final payable amount.",
                Arrays.asList(Paths.get(invoiceDir, meta.getFilename()).toString(), detailFile), session);
    }
}
