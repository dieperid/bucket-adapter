/**
 * Implementation of GCP Storage Bucket Adapter.
 */
package com.example.bucketadapter.adapter.impl;

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

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

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

    public GcpAdapterImpl() {
        this(createStorageClient());
    }

    /**
     * Constructor with parameters for testing.
     * 
     * @param storage GCP Storage client
     */
    GcpAdapterImpl(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public void upload(final String localSrc, final String remoteSrc) {
        try {
            BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

            File file = new File(localSrc);
            if (!file.exists() || !file.isFile()) {
                throw new InvalidBucketPathException(
                        "Local file does not exist: " + localSrc);
            }

            BlobId blobId = BlobId.of(bucketSrc.bucket(), bucketSrc.key());
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            storage.create(blobInfo, java.nio.file.Files.readAllBytes(file.toPath()));

        } catch (InvalidBucketPathException e) {
            throw e;
        } catch (Exception e) {
            throw new BucketOperationException(
                    "GCP error while uploading file to " + remoteSrc, e);
        }
    }

    @Override
    public void download(final String localSrc, final String remoteSrc) {
        BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

        try {
            Blob blob = storage.get(bucketSrc.bucket(), bucketSrc.key());

            if (blob == null) {
                throw new BucketObjectNotFoundException(remoteSrc);
            }

            blob.downloadTo(Path.of(localSrc));
        } catch (BucketObjectNotFoundException e) {
            throw e;
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

        String normalizedRemoteSrc = "";
        BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

        try {

            if (!recursive) {
                boolean deleted = storage.delete(bucketSrc.bucket(), bucketSrc.key());
                if (!deleted) {
                    throw new BucketObjectNotFoundException(bucketSrc.key());
                }
                return;
            }

            normalizedRemoteSrc = bucketSrc.key();

            Page<Blob> blobs = storage.list(
                    bucketSrc.bucket(),
                    Storage.BlobListOption.prefix(
                            normalizedRemoteSrc.endsWith("/") ? normalizedRemoteSrc : normalizedRemoteSrc + "/"));

            List<BlobId> toDelete = StreamSupport.stream(blobs.iterateAll().spliterator(), false)
                    .map(Blob::getBlobId)
                    .toList();

            if (toDelete.isEmpty()) {
                throw new BucketObjectNotFoundException(normalizedRemoteSrc);
            }

            storage.delete(toDelete);

        } catch (BucketObjectNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BucketOperationException(
                    "GCP error while deleting file(s) at " + remoteSrc, e);
        }
    }

    @Override
    public List<String> list(final String remoteSrc) {
        BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

        try {
            Page<Blob> blobs = storage.list(
                    bucketSrc.bucket(),
                    Storage.BlobListOption.prefix(bucketSrc.key()));

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
        BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

        try {
            return storage.get(bucketSrc.bucket(), bucketSrc.key()) != null;
        } catch (Exception e) {
            throw new BucketOperationException(
                    "GCP error while checking existence of " + remoteSrc,
                    e);
        }
    }

    @Override
    public String share(final String remoteSrc, final int expirationTime) {
        validateRemoteSrc(remoteSrc);
        validateExpiration(expirationTime);

        BucketSrc bucketSrc = AdapterHelper.extractBucketAndKey(remoteSrc);

        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketSrc.bucket(), bucketSrc.key()).build();

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
}