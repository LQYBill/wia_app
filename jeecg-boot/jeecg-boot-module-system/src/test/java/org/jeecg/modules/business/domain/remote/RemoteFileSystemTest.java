package org.jeecg.modules.business.domain.remote;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RemoteFileSystemTest {


    @Test
    void ls_test() {
        AWSCredentials credentials = new BasicAWSCredentials("AKIAVT3PM5UKZBDGFIPR", "lnJr6wfLCEEbASfUHNeNkghcnf5fsHSoeHlE5Y8+");
        RemoteFileSystem fs = new RemoteFileSystem(credentials);
        System.out.println(fs.ls());
    }

    @Test
    void ls_test_with_dir() {
        AWSCredentials credentials = new BasicAWSCredentials("AKIAVT3PM5UKZBDGFIPR", "lnJr6wfLCEEbASfUHNeNkghcnf5fsHSoeHlE5Y8+");
        RemoteFileSystem fs = new RemoteFileSystem(credentials, "test");
        System.out.println(fs.ls());
    }

    @Test
    void remote_uploda_download_is_correct() throws IOException {
        AWSCredentials credentials = new BasicAWSCredentials("AKIAVT3PM5UKZBDGFIPR", "lnJr6wfLCEEbASfUHNeNkghcnf5fsHSoeHlE5Y8+");
        RemoteFileSystem fs = new RemoteFileSystem(credentials, "test");
        System.out.println(fs.ls());

        Path tmp = Files.createTempFile("toUpload", "txt");
        Files.write(tmp, Lists.list("Hello World", "Bonjour"), Charsets.UTF_8);
        fs.cp("yes", tmp.toFile());

        Path out = Files.createTempFile("test", "txt");

        fs.wget("yes", out);
        String res = FileUtils.readFileToString(out.toFile(), "UTF-8");
        String expected = "Hello World" + System.lineSeparator() + "Bonjour" + System.lineSeparator();
        Assertions.assertEquals(expected, res);

    }


}
