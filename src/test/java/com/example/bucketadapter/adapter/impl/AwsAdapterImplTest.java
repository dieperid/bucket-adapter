package com.example.bucketadapter.adapter.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.bucketadapter.exception.BucketObjectNotFoundException;
import com.example.bucketadapter.exception.BucketOperationException;
import com.example.bucketadapter.exception.InvalidBucketPathException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class AwsAdapterImplTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private AwsAdapterImpl adapter;

    private AutoCloseable closeable;

    private final String bucketName = "test-bucket";

    private Path tempFile;

    private Path tempDirectory;

    Supplier<S3Presigner> presignerSupplier = () -> s3Presigner;

    @BeforeEach
    void setUp() throws IOException {
        closeable = MockitoAnnotations.openMocks(this);
        adapter = new AwsAdapterImpl(s3Client, bucketName, presignerSupplier);

        tempFile = Files.createTempFile("upload-test-", ".txt");
        Files.writeString(tempFile, "test content");

        tempDirectory = Files.createTempDirectory("upload-dir-");
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(tempDirectory);
        closeable.close();
    }

    // ------------------------------------------------------------------
    // Upload method tests
    // ------------------------------------------------------------------

    @Test
    void upload_shouldUploadFile_whenFileIsValid() {
        // given
        String remoteSrc = "dir/file.txt";

        // when / then
        assertDoesNotThrow(() -> adapter.upload(tempFile.toString(), remoteSrc));

        verify(s3Client, times(1)).putObject(
                argThat((PutObjectRequest req) -> req.bucket().equals(bucketName)
                        && req.key().equals(remoteSrc)),
                any(RequestBody.class));
    }

    @Test
    void upload_shouldFail_whenLocalFileDoesNotExist() {
        // given
        String missingFile = "/path/to/local/missing.txt";

        // when / then
        InvalidBucketPathException exception = assertThrows(InvalidBucketPathException.class,
                () -> adapter.upload(missingFile, "dir/file.txt"));

        assertTrue(exception.getMessage().contains("Local file does not exist"));

        verify(s3Client, never()).putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class));
    }

    @Test
    void upload_shouldFail_whenLocalPathIsDirectory() {
        // given
        String remoteSrc = "dir/file.txt";

        // when / then
        InvalidBucketPathException exception = assertThrows(InvalidBucketPathException.class,
                () -> adapter.upload(tempDirectory.toString(), remoteSrc));

        // then
        assertTrue(exception.getMessage().contains("Local file does not exist"));

        verify(s3Client, never()).putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class));
    }

    @Test
    void upload_shouldThrowBucketOperationException_whenS3Fails() {
        // given
        doThrow(S3Exception.builder()
                .statusCode(500)
                .message("AWS internal error")
                .build())
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // when / then
        BucketOperationException exception = assertThrows(BucketOperationException.class,
                () -> adapter.upload(tempFile.toString(), "dir/file.txt"));

        assertTrue(exception.getCause() instanceof S3Exception);
    }

    // ------------------------------------------------------------------
    // Download method tests
    // ------------------------------------------------------------------

    @Test
    void download_shouldDownloadFile_whenObjectExists() {
        // given
        String remoteSrc = "dir/file.txt";
        String localSrc = "/tmp/file.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        // when / then
        assertDoesNotThrow(() -> adapter.download(localSrc, remoteSrc));

        // then
        verify(s3Client, times(1))
                .headObject(any(HeadObjectRequest.class));

        verify(s3Client, times(1))
                .getObject(
                        argThat((GetObjectRequest req) -> req.bucket().equals(bucketName)
                                && req.key().equals(remoteSrc)),
                        eq(Path.of(localSrc)));
    }

    @Test
    void download_shouldFail_whenObjectDoesNotExist() {
        // given
        String remoteSrc = "missing/file.txt";
        String localSrc = "/tmp/file.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(404)
                        .build());

        // when / then
        assertThrows(BucketObjectNotFoundException.class,
                () -> adapter.download(localSrc, remoteSrc));

        // then
        verify(s3Client, times(1))
                .headObject(any(HeadObjectRequest.class));

        verify(s3Client, never())
                .getObject(any(GetObjectRequest.class), any(Path.class));
    }

    @Test
    void download_shouldThrowBucketOperationException_whenS3Fails() {
        // given
        String remoteSrc = "dir/file.txt";
        String localSrc = "/tmp/file.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        doThrow(S3Exception.builder()
                .statusCode(500)
                .message("AWS error")
                .build())
                .when(s3Client)
                .getObject(
                        any(GetObjectRequest.class),
                        any(Path.class));

        // when
        BucketOperationException exception = assertThrows(BucketOperationException.class,
                () -> adapter.download(localSrc, remoteSrc));

        // then
        assertTrue(exception.getCause() instanceof S3Exception);
    }

    // ------------------------------------------------------------------
    // Update method tests
    // ------------------------------------------------------------------

    @Test
    void update_shouldReplaceObject_whenObjectExistsAndLocalFileIsValid() throws IOException {
        // given
        String remoteSrc = "dir/file.txt";

        Path tempFile = Files.createTempFile("update-test", ".txt");
        Files.writeString(tempFile, "new content");

        AwsAdapterImpl spyAdapter = spy(adapter);

        doReturn(true)
                .when(spyAdapter)
                .doesExists(remoteSrc);

        when(s3Client.putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when / then
        assertDoesNotThrow(() -> spyAdapter.update(tempFile.toString(), remoteSrc));

        verify(spyAdapter, times(1))
                .doesExists(remoteSrc);

        verify(s3Client, times(1))
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void update_shouldFail_whenRemoteObjectDoesNotExist() throws IOException {
        // given
        String remoteSrc = "missing/file.txt";

        Path tempFile = Files.createTempFile("update-test", ".txt");
        Files.writeString(tempFile, "content");

        AwsAdapterImpl spyAdapter = spy(adapter);

        // when
        doReturn(false).when(spyAdapter).doesExists(remoteSrc);

        // then
        assertThrows(BucketObjectNotFoundException.class,
                () -> spyAdapter.update(tempFile.toString(), remoteSrc));

        verify(spyAdapter, times(1)).doesExists(remoteSrc);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void update_shouldFail_whenLocalFileIsInvalid() {
        // given
        String remoteSrc = "dir/file.txt";
        String localSrc = "/invalid/path/file.txt";

        // when / then
        assertThrows(InvalidBucketPathException.class,
                () -> adapter.update(localSrc, remoteSrc));

        // then
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void update_shouldFail_whenAwsThrowsException() throws IOException {
        // given
        String remoteSrc = "dir/file.txt";

        Path tempFile = Files.createTempFile("update-test", ".txt");
        Files.writeString(tempFile, "content");

        AwsAdapterImpl spyAdapter = spy(adapter);

        doReturn(true)
                .when(spyAdapter)
                .doesExists(remoteSrc);

        when(s3Client.putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(500)
                        .message("AWS failure")
                        .build());

        // when / then
        assertThrows(BucketOperationException.class,
                () -> spyAdapter.update(tempFile.toString(), remoteSrc));

        verify(spyAdapter, times(1))
                .doesExists(remoteSrc);

        verify(s3Client, times(1))
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // ------------------------------------------------------------------
    // List method tests
    // ------------------------------------------------------------------

    @Test
    void list_shouldReturnObjects_whenObjectsExist() {
        // given
        String prefix = "dir/";
        S3Object obj1 = S3Object.builder().key("file1.txt").build();
        S3Object obj2 = S3Object.builder().key("dir/file2.txt").build();

        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(Arrays.asList(obj1, obj2))
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // when
        List<String> result = adapter.list(prefix);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("file1.txt"));
        assertTrue(result.contains("dir/file2.txt"));

        verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void list_shouldReturnEmptyList_whenNoObjectsFound() {
        // given
        String prefix = "empty/";

        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(List.of())
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // when
        List<String> result = adapter.list(prefix);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void list_shouldThrowBucketOperationException_whenAwsFails() {
        // given
        String prefix = "dir/";
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("Internal Server Error").build());

        // when / then
        BucketOperationException exception = assertThrows(BucketOperationException.class, () -> {
            adapter.list(prefix);
        });

        assertTrue(exception.getMessage().contains("AWS S3 error") || exception.getCause() instanceof S3Exception);

        verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    // ------------------------------------------------------------------
    // doesExists method tests
    // ------------------------------------------------------------------

    @Test
    void doesExists_shouldReturnTrue_whenObjectExists() {
        // given
        String remoteSrc = "dir/file.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        // when
        boolean exists = adapter.doesExists(remoteSrc);

        // then
        assertTrue(exists);

        verify(s3Client, times(1))
                .headObject(any(HeadObjectRequest.class));
    }

    @Test
    void doesExists_shouldReturnFalse_whenObjectDoesNotExist() {
        // given
        String remoteSrc = "dir/missing.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(404)
                        .build());

        // when
        boolean exists = adapter.doesExists(remoteSrc);

        // then
        assertFalse(exists);

        verify(s3Client, times(1))
                .headObject(any(HeadObjectRequest.class));
    }

    @Test
    void doesExists_shouldFail_whenRemoteSrcIsNull() {
        // when / then
        assertThrows(InvalidBucketPathException.class,
                () -> adapter.doesExists(null));

        verify(s3Client, never())
                .headObject(any(HeadObjectRequest.class));
    }

    @Test
    void doesExists_shouldFail_whenRemoteSrcIsBlank() {
        // when / then
        assertThrows(InvalidBucketPathException.class,
                () -> adapter.doesExists("   "));

        verify(s3Client, never())
                .headObject(any(HeadObjectRequest.class));
    }

    @Test
    void doesExists_shouldThrowBucketOperationException_whenAwsFails() {
        // given
        String remoteSrc = "dir/file.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(500)
                        .message("AWS internal error")
                        .build());

        // when / then
        BucketOperationException exception = assertThrows(BucketOperationException.class,
                () -> adapter.doesExists(remoteSrc));

        // then
        assertTrue(exception.getCause() instanceof S3Exception);

        verify(s3Client, times(1))
                .headObject(any(HeadObjectRequest.class));
    }

    // ------------------------------------------------------------------
    // Share method tests
    // ------------------------------------------------------------------

    @Test
    void share_shouldReturnPresignedUrl_whenInputsAreValid() throws Exception {
        // given
        String remoteSrc = "dir/file.txt";
        int expirationTime = 3600;

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://signed-url").toURL());

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // when
        String result = adapter.share(remoteSrc, expirationTime);

        // then
        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("https://"));

        verify(s3Presigner, times(1))
                .presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void share_shouldFail_whenRemoteSrcIsNull() {
        // when / then
        assertThrows(InvalidBucketPathException.class,
                () -> adapter.share(null, 3600));

        verifyNoInteractions(s3Presigner);
    }

    @Test
    void share_shouldFail_whenRemoteSrcIsBlank() {
        // when / then
        assertThrows(InvalidBucketPathException.class,
                () -> adapter.share("   ", 3600));

        verifyNoInteractions(s3Presigner);
    }

    @Test
    void share_shouldFail_whenExpirationIsZero() {
        // when / then
        assertThrows(InvalidBucketPathException.class,
                () -> adapter.share("dir/file.txt", 0));

        verifyNoInteractions(s3Presigner);
    }

    @Test
    void share_shouldFail_whenExpirationIsTooLarge() {
        // given
        int moreThan7Days = 7 * 24 * 3600 + 1;

        // when / then
        assertThrows(InvalidBucketPathException.class,
                () -> adapter.share("dir/file.txt", moreThan7Days));

        verifyNoInteractions(s3Presigner);
    }

    @Test
    void share_shouldFail_whenAwsThrowsException() {
        // given
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(S3Exception.builder().message("AWS error").build());

        // when
        BucketOperationException exception = assertThrows(BucketOperationException.class,
                () -> adapter.share("dir/file.txt", 3600));

        // then
        assertFalse(exception.getMessage().contains("AWS"));

        verify(s3Presigner, times(1))
                .presignGetObject(any(GetObjectPresignRequest.class));
    }

}
