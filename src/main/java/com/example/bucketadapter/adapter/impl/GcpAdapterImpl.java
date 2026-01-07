/**
 * Implementation of GCP Storage Bucket Adapter.
 */
package com.example.bucketadapter.adapter.impl;

import com.example.bucketadapter.adapter.BucketAdapter;
import com.example.bucketadapter.exception.BucketObjectNotFoundException;
import com.example.bucketadapter.exception.BucketOperationException;
import com.example.bucketadapter.exception.InvalidBucketPathException;
import com.google.cloud.storage.*;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.StreamSupport;

@Component("GCP")
@Profile("!test")
public class GcpAdapterImpl implements BucketAdapter {

    private final Storage storage;
    private final String bucket;

    public GcpAdapterImpl() {
        this(
                createStorageClient(),
                resolveBucketName());
    }

    /**
     * Constructor with parameters for testing.
     * 
     * @param storage
     * @param bucket
     */
    GcpAdapterImpl(final Storage storage, final String bucket) {
        this.storage = storage;
        this.bucket = bucket;
    }

    /**
     * Resolves and validates a local filesystem path provided by the caller.
     * The resulting path is constrained to lie within a dedicated base directory,
     * preventing directory traversal and access to unexpected locations.
     *
     * @param localSrc the user-provided local path
     * @return a normalized, absolute path within the allowed base directory
     * @throws InvalidBucketPathException if the path is invalid or outside the base directory
     */
    private Path resolveAndValidateLocalPath(final String localSrc) {
        if (localSrc == null || localSrc.isBlank()) {
            throw new InvalidBucketPathException("Local path must not be empty");
        }

        // Define a base directory for all local file operations.
        // This can be adjusted or externalized as needed.
        Path baseDir = Paths.get(".").toAbsolutePath().normalize();

        // Disallow absolute paths directly provided by the caller to avoid
        // bypassing the base directory restriction.
        Path userPath = Paths.get(localSrc);
        if (userPath.isAbsolute()) {
            throw new InvalidBucketPathException("Absolute local paths are not allowed");
        }

        Path resolvedPath = baseDir.resolve(userPath).normalize();

        // Ensure the resolved path is still within the base directory.
        if (!resolvedPath.startsWith(baseDir)) {
            throw new InvalidBucketPathException("Local path is outside the allowed directory");
        }

        return resolvedPath;
    }

    @Override
    public void upload(final String localSrc, final String remoteSrc) {
        try {
            Path resolvedPath = resolveAndValidateLocalPath(localSrc);
            File file = resolvedPath.toFile();
            if (!file.exists() || !file.isFile()) {
                throw new InvalidBucketPathException(
                        "Local file does not exist: " + resolvedPath);
            }

            BlobId blobId = BlobId.of(bucket, remoteSrc);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            storage.create(blobInfo, java.nio.file.Files.readAllBytes(resolvedPath));

        } catch (Exception e) {
            throw new BucketOperationException(
                    "GCP error while uploading file to " + remoteSrc, e);
        }
    }

    @Override
    public void download(final String localSrc, final String remoteSrc) {
        try {
            Blob blob = storage.get(bucket, remoteSrc);
            if (blob == null) {
                throw new BucketObjectNotFoundException(remoteSrc);
            }

            Path targetPath = resolveAndValidateLocalPath(localSrc);
            Path parent = targetPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            blob.downloadTo(targetPath);


        } catch (Exception e) {
            throw new BucketOperationException(
                    "GCP error while downloading file from " + remoteSrc, e);
        }
    }

    @Override
    public void update(final String localSrc, final String remoteSrc) {
        // In GCP, upload overwrites by default :
        // https://docs.cloud.google.com/storage/docs/json_api/v1/objects/insert
        upload(localSrc, remoteSrc);
    }

    @Override
    public void delete(final String remoteSrc, final boolean recursive) {
        validateRemoteSrc(remoteSrc);
        validateNotRoot(remoteSrc);

        try {
            if (!recursive) {
                boolean deleted = storage.delete(bucket, remoteSrc);
                if (!deleted) {
                    throw new BucketObjectNotFoundException(remoteSrc);
                }
                return;
            }

            Page<Blob> blobs = storage.list(
                    bucket,
                    Storage.BlobListOption.prefix(
                            remoteSrc.endsWith("/") ? remoteSrc : remoteSrc + "/"));

            List<BlobId> toDelete = StreamSupport.stream(blobs.iterateAll().spliterator(), false)
                    .map(Blob::getBlobId)
                    .toList();

            if (toDelete.isEmpty()) {
                throw new BucketObjectNotFoundException(remoteSrc);
            }

            storage.delete(toDelete);

        } catch (Exception e) {
            throw new BucketOperationException(
                    "GCP error while deleting file(s) at " + remoteSrc, e);
        }
    }

    @Override
    public List<String> list(final String remoteSrc) {
        try {
            Page<Blob> blobs = storage.list(
                    bucket,
                    Storage.BlobListOption.prefix(remoteSrc));

            return StreamSupport.stream(blobs.iterateAll().spliterator(), false)
                    .map(Blob::getName)
                    .toList();

        } catch (Exception e) {
            throw new BucketOperationException(
                    "GCP error while listing files with prefix " + remoteSrc, e);
        }
    }

    @Override
    public boolean doesExists(final String remoteSrc) {
        validateRemoteSrc(remoteSrc);
        return storage.get(bucket, remoteSrc) != null;
    }

    @Override
    public String share(final String remoteSrc, final int expirationTime) {
        validateRemoteSrc(remoteSrc);
        validateExpiration(expirationTime);

        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, remoteSrc).build();

            return storage.signUrl(
                    blobInfo,
                    expirationTime,
                    java.util.concurrent.TimeUnit.SECONDS,
                    Storage.SignUrlOption.withV4Signature()).toString();

        } catch (Exception e) {
            throw new BucketOperationException(
                    "GCP error while generating signed URL for " + remoteSrc, e);
        }
    }

    // ---------------- PRIVATE ---------------- //

    /**
     * Create GCP Storage client.
     * 
     * @return Storage client
     */
    private static Storage createStorageClient() {
        String credentialsPath = getConfig(
                "GOOGLE_APPLICATION_CREDENTIALS",
                "GCP credentials path");

        try (InputStream in = Files.newInputStream(Paths.get(credentialsPath))) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in);
            return StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load GCP credentials", e);
        }
    }

    /**
     * Resolve GCP bucket name from environment or system properties.
     * 
     * @return Bucket name
     */
    private static String resolveBucketName() {
        return getConfig("GCP_BUCKET_NAME", "GCP bucket name");
    }

    /**
     * Get configuration value from environment variable or system property.
     * 
     * @param envVar Environment variable / system property name
     * @param name   Configuration name for error message
     * @return Configuration value
     */
    private static String getConfig(final String envVar, final String name) {
        String value = System.getenv(envVar);
        if (value == null || value.isBlank()) {
            value = System.getProperty(envVar);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not configured");
        }
        return value;
    }

    /**
     * Validate that remoteSrc exists
     * 
     * @param remoteSrc
     */
    private void validateRemoteSrc(final String remoteSrc) {
        if (remoteSrc == null || remoteSrc.isBlank()) {
            throw new InvalidBucketPathException("remoteSrc must not be empty");
        }
    }

    /**
     * Validate that remoteSrc is not root
     * 
     * @param remoteSrc
     */
    private void validateNotRoot(final String remoteSrc) {
        if ("/".equals(remoteSrc)) {
            throw new InvalidBucketPathException("Root path '/' is forbidden");
        }
    }

    /**
     * Validate expiration time for signed URL
     * 
     * @param expirationTime
     */
    private void validateExpiration(final int expirationTime) {
        if (expirationTime <= 0 || expirationTime > 7 * 24 * 3600) {
            throw new InvalidBucketPathException(
                    "expirationTime must be between 1 second and 7 days");
        }
    }
}
