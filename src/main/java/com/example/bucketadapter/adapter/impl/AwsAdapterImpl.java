package com.example.bucketadapter.adapter.impl;

import org.springframework.stereotype.Component;

import com.example.bucketadapter.adapter.BucketAdapter;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AwsAdapterImpl implements BucketAdapter {

    private final S3Client s3Client;
    private final String bucket;

    public AwsAdapterImpl() {
        this(
                createS3Client(),
                resolveBucketName());
    }

    /**
     * Constructor with parameters for testing purposes.
     * 
     * @param s3Client - S3 client
     * @param bucket   - S3 bucket name
     */
    AwsAdapterImpl(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void upload(String localSrc, String remoteSrc) {
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(remoteSrc)
                .build(),
                RequestBody.fromFile(new File(localSrc)));
    }

    @Override
    public void download(String localSrc, String remoteSrc) {
        s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(remoteSrc)
                .build(),
                Paths.get(localSrc));
    }

    @Override
    public void delete(String remoteSrc, boolean recursive) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(remoteSrc)
                .build());
    }

    @Override
    public List<String> list(String remoteSrc) {
        ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(remoteSrc)
                .build());

        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * Create S3 client with resolved region and default credentials provider.
     * 
     * @return S3 client
     */
    private static S3Client createS3Client() {
        String region = resolveRegion();

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }

    /**
     * Resolve S3 bucket name from system property or environment variable.
     * 
     * @return S3 bucket name
     */
    private static String resolveBucketName() {
        return getConfig(
                "aws.s3.bucket.name",
                "AWS_S3_BUCKET_NAME",
                "S3 bucket name");
    }

    /**
     * Resolve AWS S3 region from system property or environment variable.
     * 
     * @return AWS S3 region
     */
    private static String resolveRegion() {
        return getConfig(
                "aws.s3.region",
                "AWS_S3_REGION",
                "S3 region");
    }

    /**
     * Utility method to get configuration from system property or environment
     * variable.
     * 
     * @param systemProperty - system property name
     * @param envVar         - environment variable name
     * @param configName     - configuration descriptive name
     * @return configuration value
     */
    private static String getConfig(String systemProperty, String envVar, String configName) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(envVar);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    configName + " is not configured. Set " + systemProperty +
                            " property or " + envVar + " env variable.");
        }
        return value;
    }
}
