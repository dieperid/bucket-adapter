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
import com.example.bucketadapter.helper.AdapterHelper;

import static com.example.bucketadapter.helper.ConfigHelper.getConfig;
import static com.example.bucketadapter.helper.AdapterHelper.validateExpiration;
import static com.example.bucketadapter.helper.AdapterHelper.validateNotRoot;
import static com.example.bucketadapter.helper.AdapterHelper.validateRemoteSrc;
import static com.example.bucketadapter.helper.AdapterHelper.BucketSrc;

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
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component("AWS")
@Profile("!test")
public class AwsAdapterImpl implements BucketAdapter {

    private final S3Client s3Client;
    private final Supplier<S3Presigner> presignerSupplier;

    public AwsAdapterImpl() {
        this(
                createS3Client(),
                AwsAdapterImpl::createS3Presigner);
    }

    /**
     * Constructor with parameters for testing purposes.
     * 
     * @param s3Client - S3 client
     * @param bucket   - S3 bucket name
     */
    AwsAdapterImpl(S3Client s3Client, Supplier<S3Presigner> presignerSupplier) {
        this.s3Client = s3Client;
        this.presignerSupplier = presignerSupplier;
    }

    @Override
    public void upload(String localSrc, String remoteSrc) {
        try {
            BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

            File file = new File(localSrc);
            if (!file.exists() || !file.isFile()) {
                throw new InvalidBucketPathException(
                        "Local file does not exist: " + localSrc);
            }

            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketSrc.bucket())
                    .key(bucketSrc.key())
                    .build(),
                    RequestBody.fromFile(file));
        } catch (S3Exception e) {
            throw new BucketOperationException(
                    "AWS S3 error while uploading file to " + remoteSrc, e);
        }
    }

    @Override
    public void download(String localSrc, String remoteSrc) {
        validateRemoteSrc(remoteSrc);

        BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);
        AdapterHelper.validateKeyRequired(bucketSrc);

        try {
            s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketSrc.bucket())
                    .key(bucketSrc.key())
                    .build(),
                    Paths.get(localSrc));
        } catch (S3Exception e) {
            throw new BucketOperationException(
                    "AWS S3 error while downloading file from " + remoteSrc, e);
        }
    }

    @Override
    public void update(String localSrc, String remoteSrc) {
        try {
            File file = new File(localSrc);
            if (!file.exists() || !file.isFile()) {
                throw new InvalidBucketPathException(
                        "Local file does not exist or is not a file: " + localSrc);
            }

            // Check if the object exists
            if (!doesExists(remoteSrc)) {
                throw new BucketObjectNotFoundException(remoteSrc);
            }

            BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

            // Replace the existing object
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketSrc.bucket())
                    .key(bucketSrc.key())
                    .build(),
                    RequestBody.fromFile(new File(localSrc)));
        } catch (S3Exception e) {
            throw new BucketOperationException(
                    "AWS S3 error while updating file at " + remoteSrc, e);
        }
    }

    @Override
    public void delete(String remoteSrc, boolean recursive) {
        validateRemoteSrc(remoteSrc);
        validateNotRoot(remoteSrc);

        String normalizedRemoteSrc = "";

        try {
            BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

            if (!recursive) {
                // delete a single object
                if (!doesExists(remoteSrc)) {
                    throw new BucketObjectNotFoundException(remoteSrc);
                }

                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketSrc.bucket())
                        .key(bucketSrc.key())
                        .build());
                return;
            }

            normalizedRemoteSrc = bucketSrc.key();

            // recursive delete: delete all objects with prefix
            String prefix = normalizedRemoteSrc.endsWith("/") ? normalizedRemoteSrc : normalizedRemoteSrc + "/";

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(
                    ListObjectsV2Request.builder()
                            .bucket(bucketSrc.bucket())
                            .prefix(prefix)
                            .build());

            if (listResponse.contents().isEmpty()) {
                throw new BucketObjectNotFoundException(normalizedRemoteSrc);
            }

            List<ObjectIdentifier> objectsToDelete = listResponse.contents().stream()
                    .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                    .toList();

            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucketSrc.bucket())
                    .delete(Delete.builder().objects(objectsToDelete).build())
                    .build());
        } catch (S3Exception e) {
            throw new BucketOperationException(
                    "AWS S3 error while deleting file(s) at " + normalizedRemoteSrc, e);
        }
    }

    @Override
    public List<String> list(String remoteSrc) {
        try {
            BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucketSrc.bucket())
                    .prefix(AdapterHelper.normalizePrefix(bucketSrc.key()))
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
            BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketSrc.bucket())
                    .key(bucketSrc.key())
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

        try (S3Presigner presigner = presignerSupplier.get()) {
            BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);
            AdapterHelper.validateKeyRequired(bucketSrc);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketSrc.bucket())
                    .key(bucketSrc.key())
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
     * Create S3 presigner with resolved region and default credentials provider.
     * 
     * @return S3 presigner
     */
    private static S3Presigner createS3Presigner() {
        String accessKey = getConfig("AWS_ACCESS_KEY_ID", "AWS Access Key ID");
        String secretKey = getConfig("AWS_SECRET_ACCESS_KEY", "AWS Secret Access Key");

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Presigner.builder()
                .region(Region.of(resolveRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
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
}
