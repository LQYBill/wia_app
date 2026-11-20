package org.jeecg.modules.business.domain.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.impl.purchase.AutoPurchasePreparationService;
import org.jeecg.modules.business.service.impl.purchase.AutoPurchaseDeliveryService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import javax.mail.internet.InternetAddress;

@Slf4j
@DisallowConcurrentExecution
public class AutoPurchaseJob implements Job {
    @Autowired private IClientService clients;
    @Autowired private AutoPurchasePreparationService preparation;
    @Autowired private AutoPurchaseDeliveryService delivery;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Parameters parameters = parse(context.getMergedJobDataMap().getString("parameter"));
            List<Client> targets = clients.getClientsByCodes(new ArrayList<>(parameters.clients.keySet()));
            Set<String> found = targets.stream().map(Client::getInternalCode).collect(Collectors.toSet());
            if (!found.equals(parameters.clients.keySet())) throw new IllegalArgumentException("Some configured client codes were not found");
            if (targets.size() != found.size()) throw new IllegalArgumentException("Duplicate client internal codes");
            String day = LocalDate.now(parameters.zone).toString();
            List<String> failures = new ArrayList<>();
            for (Client client : targets) {
                try {
                    ClientParameters settings = parameters.clients.get(client.getInternalCode());
                    delivery.deliver(preparation.prepare(client, day, settings.email,
                            settings.autoMarkPaid, settings.mabangUsername));
                } catch (Exception e) {
                    log.error("Automatic purchase failed for client {}", client.getInternalCode(), e);
                    failures.add(client.getInternalCode() + ": " + e.getMessage());
                }
            }
            if (!failures.isEmpty()) throw new JobExecutionException(String.join("; ", failures));
        } catch (JobExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new JobExecutionException("AutoPurchaseJob failed: " + e.getMessage(), e);
        }
    }

    static Parameters parse(String raw) throws Exception {
        if (raw == null || raw.trim().isEmpty()) throw new IllegalArgumentException("AutoPurchaseJob parameters required");
        JSONObject json = JSON.parseObject(raw);
        if (json == null || !(json.get("clients") instanceof JSONObject)) throw new IllegalArgumentException("clients must map client codes to purchase settings");
        if (json.containsKey("clientEmails") || json.containsKey("mabangUsername") || json.containsKey("autoMarkPaid")) {
            throw new IllegalArgumentException("Configure email, mabangUsername and autoMarkPaid inside each clients entry");
        }
        Parameters result = new Parameters();
        JSONObject configuredClients = json.getJSONObject("clients");
        if (configuredClients.isEmpty()) throw new IllegalArgumentException("At least one client configuration is required");
        for (String code : configuredClients.keySet()) {
            Object value = configuredClients.get(code);
            if (code.trim().isEmpty() || !code.equals(code.trim()) || !(value instanceof JSONObject)) throw new IllegalArgumentException("Invalid purchase settings for client " + code);
            JSONObject settings = (JSONObject) value;
            Object recipient = settings.get("email");
            if (!(recipient instanceof String)) throw new IllegalArgumentException("email is required for client " + code);
            String email = ((String) recipient).trim();
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            if (!email.equals(address.getAddress()) || email.contains("\r") || email.contains("\n") || !email.contains("@") || email.length() > 254) {
                throw new IllegalArgumentException("Invalid email for client " + code);
            }
            ClientParameters client = new ClientParameters();
            client.email = email;
            Object paid = settings.get("autoMarkPaid");
            if (settings.containsKey("autoMarkPaid") && !(paid instanceof Boolean)) throw new IllegalArgumentException("autoMarkPaid must be boolean for client " + code);
            client.autoMarkPaid = Boolean.TRUE.equals(paid);
            Object username = settings.get("mabangUsername");
            if (!(username instanceof String) || ((String) username).trim().isEmpty() || ((String) username).length() > 100) {
                throw new IllegalArgumentException("mabangUsername is required for client " + code);
            }
            client.mabangUsername = ((String) username).trim();
            result.clients.put(code, client);
        }
        result.zone = ZoneId.of(json.getString("timezone") == null ? "Europe/Paris" : json.getString("timezone"));
        return result;
    }

    static class Parameters {
        final Map<String, ClientParameters> clients = new LinkedHashMap<>();
        ZoneId zone;
    }

    static class ClientParameters {
        String email;
        boolean autoMarkPaid;
        String mabangUsername;
    }
}
