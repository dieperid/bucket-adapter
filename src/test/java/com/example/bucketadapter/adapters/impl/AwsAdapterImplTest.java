package com.example.bucketadapter.adapters.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;

import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AwsAdapterImplTest {

    @Spy
    private S3Client s3ClientSpy;

    private AwsAdapterImpl awsAdapter;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        s3ClientSpy = mock(S3Client.class);

        S3ClientBuilder builderMock = mock(S3ClientBuilder.class);
        when(builderMock.region(any())).thenReturn(builderMock);
        when(builderMock.credentialsProvider(any())).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(s3ClientSpy);

        when(s3ClientSpy.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        when(s3ClientSpy.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        when(s3ClientSpy.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(java.util.Collections.emptyList()).build());

        // Mock getObject de façon type-safe
        Path fakePath = Paths.get("local.txt");

        Answer<Path> getObjectAnswer = invocation -> fakePath;
        doAnswer(getObjectAnswer)
                .when(s3ClientSpy)
                .getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));

        try (var mockedStatic = mockStatic(S3Client.class)) {
            mockedStatic.when(S3Client::builder).thenReturn(builderMock);
            awsAdapter = new AwsAdapterImpl();
        }
    }

    @Test
    void testUpload() throws Exception {
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("local", ".txt");
        java.nio.file.Files.writeString(tempFile, "dummy content");

        awsAdapter.upload(tempFile.toString(), "remote.txt");

        verify(s3ClientSpy, times(1))
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        java.nio.file.Files.deleteIfExists(tempFile);
    }

    @Test
    void testDownload() {
        awsAdapter.download("local.txt", "remote.txt");

        verify(s3ClientSpy).getObject(
                any(GetObjectRequest.class),
                ArgumentMatchers.<ResponseTransformer<GetObjectResponse, Path>>any());
    }

    @Test
    void testDelete() {
        awsAdapter.delete("remote.txt", false);

        verify(s3ClientSpy, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testList() {
        // Mock response du S3Client
        ListObjectsV2Response responseMock = mock(ListObjectsV2Response.class);
        S3Object obj1 = S3Object.builder().key("file1").build();
        S3Object obj2 = S3Object.builder().key("file2").build();
        when(responseMock.contents()).thenReturn(Arrays.asList(obj1, obj2));
        doReturn(responseMock).when(s3ClientSpy).listObjectsV2(any(ListObjectsV2Request.class));

        List<String> result = awsAdapter.list("prefix/");

        assertEquals(2, result.size());
        assertTrue(result.contains("file1"));
        assertTrue(result.contains("file2"));

        verify(s3ClientSpy, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }
}
