/**
 * Implementation of AWS S3 Bucket Adapter.
 */
package com.example.bucketadapter.adapter.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.bucketadapter.adapter.BucketAdapter;
import com.example.bucketadapter.exception.BucketObjectNotFoundException;
import com.example.bucketadapter.exception.BucketOperationException;
import com.example.bucketadapter.exception.InvalidBucketPathException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Component("AWS")
@Profile("!test")
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
        try {

            File file = new File(localSrc);
            if (!file.exists() || !file.isFile()) {
                throw new InvalidBucketPathException(
                        "Local file does not exist: " + localSrc);
            }

            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(remoteSrc)
                    .build(),
                    RequestBody.fromFile(file));
        } catch (S3Exception e) {
            throw new BucketOperationException(
                    "AWS S3 error while uploading file to " + remoteSrc, e);
        }
    }

    @Override
    public void download(String localSrc, String remoteSrc) {
        // First, check if the object exists
        if (!doesExists(remoteSrc)) {
            throw new BucketObjectNotFoundException(remoteSrc);
        }

        s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(remoteSrc)
                .build(),
                Paths.get(localSrc));
    }

    @Override
    public void update(String localSrc, String remoteSrc) {
        // First, check if the object exists
        if (!doesExists(remoteSrc)) {
            throw new BucketObjectNotFoundException(remoteSrc);
        }

        // Replace the existing object
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(remoteSrc)
                .build(),
                RequestBody.fromFile(new File(localSrc)));
    }

    @Override
    public void delete(String remoteSrc, boolean recursive) {
        validateRemoteSrc(remoteSrc);
        validateNotRoot(remoteSrc);

        if (!recursive) {
            // delete a single object
            if (!doesExists(remoteSrc)) {
                throw new BucketObjectNotFoundException(remoteSrc);
            }

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(remoteSrc)
                    .build());
            return;
        }

        // recursive delete: delete all objects with prefix
        String prefix = remoteSrc.endsWith("/") ? remoteSrc : remoteSrc + "/";

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .build());

        if (listResponse.contents().isEmpty()) {
            throw new BucketObjectNotFoundException(remoteSrc);
        }

        List<ObjectIdentifier> objectsToDelete = listResponse.contents().stream()
                .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                .toList();

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(objectsToDelete).build())
                .build());
    }

    @Override
    public List<String> list(String remoteSrc) {
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(remoteSrc)
                    .build());

            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            throw new BucketOperationException(
                    "AWS S3 error while listing files with prefix " + remoteSrc, e);
        }
    }

    @Override
    public boolean doesExists(String remoteSrc) {
        validateRemoteSrc(remoteSrc);

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(remoteSrc)
                    .build());
            return true;

        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new BucketOperationException(
                    "AWS S3 error while checking existence of " + remoteSrc, e);
        }
    }

    @Override
    public String share(String remoteSrc, int expirationTime) {
        validateRemoteSrc(remoteSrc);
        validateExpiration(expirationTime);

        String accessKey = getConfig("AWS_ACCESS_KEY_ID", "AWS Access Key ID");
        String secretKey = getConfig("AWS_SECRET_ACCESS_KEY", "AWS Secret Access Key");

        if (accessKey == null || secretKey == null) {
            throw new IllegalStateException("AWS credentials are not set in environment variables");
        }

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(resolveRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(remoteSrc)
                    .build();

            // Create presigned request
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(java.time.Duration.ofSeconds(expirationTime))
                            .getObjectRequest(getObjectRequest)
                            .build());

            // Return shared URL
            return presignedRequest.url().toString();

        } catch (S3Exception e) {
            throw new BucketOperationException(
                    "Error generating pre-signed URL for " + remoteSrc, e);
        }
    }

    // ------------------- PRIVATE METHODS ------------------ //

    /**
     * Create S3 client with resolved region and default credentials provider.
     * 
     * @return S3 client
     */
    private static S3Client createS3Client() {
        String region = resolveRegion();
        String accessKey = getConfig("AWS_ACCESS_KEY_ID", "AWS Access Key ID");
        String secretKey = getConfig("AWS_SECRET_ACCESS_KEY", "AWS Secret Access Key");

        if (accessKey == null || secretKey == null) {
            throw new IllegalStateException("AWS credentials are not set in environment variables");
        }

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    /**
     * Resolve S3 bucket name from system property or environment variable.
     * 
     * @return S3 bucket name
     */
    private static String resolveBucketName() {
        return getConfig(
                "AWS_BUCKET_NAME",
                "S3 bucket name");
    }

    /**
     * Resolve AWS S3 region from system property or environment variable.
     * 
     * @return AWS S3 region
     */
    private static String resolveRegion() {
        return getConfig(
                "AWS_REGION",
                "S3 region");
    }

    /**
     * Utility method to get configuration from system property or environment
     * variable.
     * 
     * @param envVar     - environment variable name
     * @param configName - configuration descriptive name
     * @return configuration value
     */
    private static String getConfig(String envVar, String configName) {
        // 1. FIRST: Check Docker/container environment variables (highest priority)
        String value = System.getenv(envVar);

        // 2. SECOND: Check system properties (set by DotenvInitializer from .env)
        if (value == null || value.isBlank()) {
            value = System.getProperty(envVar); // AWS_BUCKET_NAME
        }

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    configName + " is not configured.\n" +
                            "When running locally: Add to .env file as " + envVar + "=value\n" +
                            "When running in Docker: Set environment variable " + envVar);
        }
        return value;
    }

    /**
     * Validate that remoteSrc exists
     * 
     * @param remoteSrc
     */
    private void validateRemoteSrc(String remoteSrc) {
        if (remoteSrc == null || remoteSrc.isBlank()) {
            throw new InvalidBucketPathException("remoteSrc must not be null or empty");
        }
    }

    /**
     * Valide that remoteSrc is not root folder
     * 
     * @param remoteSrc
     */
    private void validateNotRoot(String remoteSrc) {
        if ("/".equals(remoteSrc)) {
            throw new InvalidBucketPathException("Root path '/' is forbidden");
        }
    }

    /**
     * Valide expiration time
     * 
     * @param expirationTime
     */
    private void validateExpiration(int expirationTime) {
        if (expirationTime <= 0 || expirationTime > 7 * 24 * 3600) {
            throw new InvalidBucketPathException(
                    "expirationTime must be between 1 second and 7 days");
        }
    }
}
