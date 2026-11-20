package org.jeecg.modules.business.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EmailServiceAttachmentsTest {
    @TempDir Path directory;

    @Test void includesBothFilesWithTheirContentsInOneEmail() throws Exception {
        Path invoice = Files.write(directory.resolve("invoice.xlsx"), new byte[]{1, 2, 3});
        Path detail = Files.write(directory.resolve("purchase_details.xlsx"), new byte[]{4, 5, 6});
        EmailServiceImpl service = new EmailServiceImpl();
        ReflectionTestUtils.setField(service, "env", new MockEnvironment().withProperty("spring.mail.username", "sender@example.com"));
        MimeMessage message = service.createMessageWithAttachments("client@example.com", "Invoice", "Invoice and details",
                Arrays.asList(invoice.toString(), detail.toString()), Session.getInstance(new Properties()));
        message.saveChanges();
        Multipart parts = (Multipart) message.getContent();
        assertEquals(3, parts.getCount());
        assertEquals("invoice.xlsx", parts.getBodyPart(1).getFileName());
        assertEquals("purchase_details.xlsx", parts.getBodyPart(2).getFileName());
        try (java.io.InputStream invoiceContent = parts.getBodyPart(1).getInputStream();
             java.io.InputStream detailContent = parts.getBodyPart(2).getInputStream()) {
            assertArrayEquals(new byte[]{1, 2, 3}, org.apache.commons.io.IOUtils.toByteArray(invoiceContent));
            assertArrayEquals(new byte[]{4, 5, 6}, org.apache.commons.io.IOUtils.toByteArray(detailContent));
        }
        assertEquals("client@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("sender@example.com", message.getRecipients(Message.RecipientType.CC)[0].toString());
    }
}
