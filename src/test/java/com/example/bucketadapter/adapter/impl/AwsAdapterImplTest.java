package com.example.bucketadapter.adapter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.bucketadapter.exception.BucketOperationException;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AwsAdapterImplTest {

    @Mock
    private S3Client mockS3Client;

    @InjectMocks
    private AwsAdapterImpl adapter;

    private AutoCloseable closeable;

    private final String bucketName = "test-bucket";

    private Path tempFile;

    private Path tempDirectory;

    @BeforeEach
    void setUp() throws IOException {
        closeable = MockitoAnnotations.openMocks(this);
        adapter = new AwsAdapterImpl(mockS3Client, bucketName);

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

        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // when
        List<String> result = adapter.list(prefix);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("file1.txt"));
        assertTrue(result.contains("dir/file2.txt"));

        verify(mockS3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void list_shouldReturnEmptyList_whenNoObjectsFound() {
        // given
        String prefix = "empty/";

        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(List.of())
                .build();

        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // when
        List<String> result = adapter.list(prefix);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(mockS3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void list_shouldThrowBucketOperationException_whenAwsFails() {
        // given
        String prefix = "dir/";
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("Internal Server Error").build());

        // when / then
        BucketOperationException exception = assertThrows(BucketOperationException.class, () -> {
            adapter.list(prefix);
        });

        assertTrue(exception.getMessage().contains("AWS S3 error") || exception.getCause() instanceof S3Exception);

        verify(mockS3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }
}
