package org.jeecg.modules.business.domain.job;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.service.EmailService;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.ISkuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.mail.Session;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SendInventoryJobTest {
    @TempDir
    Path directory;

    private final IClientService clients = mock(IClientService.class);
    private final ISkuService skus = mock(ISkuService.class);
    private final EmailService emails = mock(EmailService.class);

    private SendInventoryJob job() {
        SendInventoryJob job = new SendInventoryJob();
        ReflectionTestUtils.setField(job, "clientService", clients);
        ReflectionTestUtils.setField(job, "skuService", skus);
        ReflectionTestUtils.setField(job, "emailService", emails);
        ReflectionTestUtils.setField(job, "INVENTORY_DIR", directory.toString());
        ReflectionTestUtils.setField(job, "env", mock(Environment.class));
        return job;
    }

    private JobExecutionContext context(String parameter) {
        JobDataMap data = new JobDataMap();
        data.put("parameter", parameter);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        return context;
    }

    @Test
    void sendsEachInventoryToConfiguredEmailEvenWhenProfileEmailIsMissing() throws Exception {
        Client a = new Client();
        a.setInternalCode("A");
        a.setEmail("profile@example.com");
        Client b = new Client();
        b.setInternalCode("B");
        when(clients.getClientsByCodes(anyList())).thenReturn(Arrays.asList(a, b));
        when(skus.getInventoryByClientCode(anyString())).thenReturn(Collections.emptyList());
        when(emails.getMailSender()).thenReturn(new Properties());
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("client/dailyInventory.ftl", "Inventory");
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setTemplateLoader(loader);
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setConfiguration(configuration);
        when(emails.freemarkerClassLoaderConfig()).thenReturn(configurer);

        job().execute(context("{\"clientEmails\":{\"A\":\" inventory-a@example.com \",\"B\":\"inventory-b@example.com\"}}"));

        verify(clients).getClientsByCodes(argThat(codes -> codes.size() == 2 && codes.containsAll(Arrays.asList("A", "B"))));
        verify(emails).sendMessageWithAttachment(eq("inventory-a@example.com"), anyString(), anyString(), contains("A_("), any(Session.class));
        verify(emails).sendMessageWithAttachment(eq("inventory-b@example.com"), anyString(), anyString(), contains("B_("), any(Session.class));
        verify(emails, never()).sendMessageWithAttachment(eq("profile@example.com"), anyString(), anyString(), anyString(), any(Session.class));
    }

    @Test
    void clientsCanRestrictConfiguredRecipients() throws Exception {
        when(clients.getClientsByCodes(Collections.singletonList("B"))).thenReturn(Collections.emptyList());
        job().execute(context("{\"clients\":[\"B\"],\"clientEmails\":{\"A\":\"a@example.com\",\"B\":\"b@example.com\"}}"));
        verify(clients).getClientsByCodes(Collections.singletonList("B"));
        verifyNoInteractions(emails);
    }

    @Test
    void invalidParametersFailBeforeLoadingOrSendingInventory() {
        for (String parameter : Arrays.asList(null, "", "{", "{}", "{\"clients\":[\"A\"]}",
                "{\"clientEmails\":{}}", "{\"clientEmails\":{\"A\":null}}",
                "{\"clientEmails\":{\"A\":\"invalid\"}}",
                "{\"clients\":[\"B\"],\"clientEmails\":{\"A\":\"a@example.com\"}}")) {
            assertThrows(JobExecutionException.class, () -> job().execute(context(parameter)));
        }
        verifyNoInteractions(clients, skus, emails);
    }
}
