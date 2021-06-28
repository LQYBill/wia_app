package org.jeecg.modules.business.domain.remote;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class provide operations for manipulating Amazon S3 server by simulating terminal.
 * Not thread safe.
 */
public class RemoteFileSystem {
    private final static String BUCKET_NAME = "mabangerp-orders";

    private final static String ACCESS_KEY = "AKIAVT3PM5UKZBDGFIPR";

    private final static String SECRET_KEY = "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";

    private final static AWSCredentials awsCredentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);

    private final static AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.EU_WEST_3)
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .build();

    private final LinkedList<String> prefix;

    /**
     * Open a new terminal.
     */
    public RemoteFileSystem() {
        this.prefix = new LinkedList<>();
    }

    /**
     * Open a terminal in the specific directory, even the directory doesn't existe,
     *
     * @param first the path or the first part of the directory
     * @param more  additional path of the directory in case of existence
     */
    public RemoteFileSystem(String first, String... more) {
        this();
        prefix.add(first);
        prefix.addAll(Arrays.asList(more.clone()));
    }

    /**
     * Equivalent to "cd dir"
     *
     * @param dir the directory to open
     */
    public void cd(String dir) {
        switch (dir) {
            case "..": {
                prefix.removeLast();
            }
            case ".": {
                /* rien fait */
            }
            default: {
                prefix.add(dir);
            }
        }
    }

    /**
     * Equivalent to "cd /"
     */
    public void cdRoot() {
        prefix.clear();
    }

    /**
     * Copy the file to this remote system.
     *
     * @param name filename
     * @param file the file to copy
     */
    public void cp(String name, File file) {
        s3.putObject(BUCKET_NAME, collapse(name), file);
    }

    /**
     * Plural version cp.
     *
     * @param files files to copy
     */
    public void cp(Map<String, File> files) {
        files.forEach(this::cp);
    }

    /**
     * Remove the file represented by the key
     *
     * @param filename name of the file to remove
     */
    public void rm(String filename) {
        s3.deleteObject(BUCKET_NAME, collapse(filename));
    }

    public void rm(List<String> filenames) {
        filenames.forEach(this::rm);
    }

    /**
     * List all content of the file system.
     *
     * @return list of content of current dir
     */
    public static List<String> ls() {
        ListObjectsV2Result result = s3.listObjectsV2(BUCKET_NAME);
        return result.getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toList());
    }

    private String collapse(String key) {
        return String.join("/", prefix) + "/" + key;
    }
}
