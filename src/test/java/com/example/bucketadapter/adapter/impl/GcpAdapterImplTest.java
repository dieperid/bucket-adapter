package com.example.bucketadapter.adapter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bucketadapter.exception.BucketObjectNotFoundException;
import com.example.bucketadapter.exception.BucketOperationException;
import com.example.bucketadapter.exception.InvalidBucketPathException;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;

@ExtendWith(MockitoExtension.class)
public class GcpAdapterImplTest {

    private final String BUCKET = "test-bucket";

    @Mock
    private Storage storage;

    @Mock
    private Page<Blob> page;

    @Mock
    private Blob blob1;

    @Mock
    private Blob blob2;

    @InjectMocks
    private GcpAdapterImpl adapter;

    @BeforeEach
    void setUp() throws IOException {
        adapter = new GcpAdapterImpl(storage, BUCKET);
    }

    // ------------------------------------------------------------------
    // Upload method tests
    // ------------------------------------------------------------------

    @Test
    void upload_shouldCallStorageCreate_whenFileIsValid() throws Exception {
        // Given
        String remotePath = "dir/file.txt";
        Path tempFile = Files.createTempFile("testfile", ".txt");
        Files.writeString(tempFile, "dummy content");

        // When
        adapter.upload(tempFile.toString(), remotePath);

        // Then
        verify(storage).create(
                any(BlobInfo.class),
                eq(Files.readAllBytes(tempFile)));

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void upload_shouldThrowInvalidBucketPathException_whenFileDoesNotExist() {
        // Given
        String missingFile = "/path/to/local/missing.txt";
        String remotePath = "dir/file.txt";

        // When / Then
        InvalidBucketPathException ex = assertThrows(
                InvalidBucketPathException.class,
                () -> adapter.upload(missingFile, remotePath));

        // Then
        assertTrue(ex.getMessage().contains("does not exist"));
        verify(storage, org.mockito.Mockito.never()).create(any(BlobInfo.class), any());
    }

    @Test
    void upload_shouldThrowInvalidBucketPathException_whenLocalPathIsDirectory() throws Exception {
        // Given
        Path tempDir = Files.createTempDirectory("tempDir");
        String remotePath = "dir/file.txt";

        // When / Then
        InvalidBucketPathException ex = assertThrows(
                InvalidBucketPathException.class,
                () -> adapter.upload(tempDir.toString(), remotePath));

        // Then
        assertTrue(ex.getMessage().contains("does not exist"));
        verify(storage, org.mockito.Mockito.never()).create(any(BlobInfo.class), any());

        // Cleanup
        Files.deleteIfExists(tempDir);
    }

    @Test
    void upload_shouldThrowBucketOperationException_whenGcpFails() throws Exception {
        // Given
        Path tempFile = Files.createTempFile("testfile", ".txt");
        Files.writeString(tempFile, "dummy content");
        String remotePath = "dir/file.txt";

        when(storage.create(any(BlobInfo.class), any())).thenThrow(new RuntimeException("GCP failure"));

        // When / Then
        BucketOperationException ex = assertThrows(
                BucketOperationException.class,
                () -> adapter.upload(tempFile.toString(), remotePath));

        assertTrue(ex.getMessage().contains("GCP error"));

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    // ------------------------------------------------------------------
    // Download method tests
    // ------------------------------------------------------------------

    @Test
    void download_shouldWriteFile_whenObjectExists() throws Exception {
        // Given
        String remoteSrc = "dir/file.txt";
        String localSrc = "/tmp/localFile.txt";

        when(storage.get(BUCKET, remoteSrc)).thenReturn(blob1);

        // Mock downloadTo pour ne rien faire
        doNothing().when(blob1).downloadTo(Path.of(localSrc));

        // When
        adapter.download(localSrc, remoteSrc);

        // Then
        verify(storage).get(BUCKET, remoteSrc);
        verify(blob1).downloadTo(Path.of(localSrc));
    }

    @Test
    void download_shouldThrowBucketObjectNotFoundException_whenObjectDoesNotExist() {
        // Given
        String remoteSrc = "missing/file.txt";
        String localSrc = "/tmp/localFile.txt";

        when(storage.get(BUCKET, remoteSrc)).thenReturn(null);

        // When / Then
        BucketObjectNotFoundException ex = assertThrows(
                BucketObjectNotFoundException.class,
                () -> adapter.download(localSrc, remoteSrc));

        assertTrue(ex.getMessage().contains(remoteSrc));
        verify(storage).get(BUCKET, remoteSrc);
    }

    @Test
    void download_shouldThrowBucketOperationException_whenGcpFails() throws Exception {
        // Given
        String remoteSrc = "dir/file.txt";
        String localSrc = "/tmp/localFile.txt";

        when(storage.get(BUCKET, remoteSrc)).thenReturn(blob1);
        doThrow(new RuntimeException("GCP download failure"))
                .when(blob1).downloadTo(Path.of(localSrc));

        // When / Then
        BucketOperationException ex = assertThrows(
                BucketOperationException.class,
                () -> adapter.download(localSrc, remoteSrc));

        assertTrue(ex.getMessage().contains("GCP error while downloading"));
        verify(storage).get(BUCKET, remoteSrc);
        verify(blob1).downloadTo(Path.of(localSrc));
    }

    // ------------------------------------------------------------------
    // Update method tests
    // ------------------------------------------------------------------

    @Test
    void update_shouldDelegateToUpload_whenCalled() throws Exception {
        // Given
        File tempFile = File.createTempFile("test-file", ".txt");
        tempFile.deleteOnExit();

        // When
        adapter.update(tempFile.getAbsolutePath(), "dir/file.txt");

        // Then
        verify(storage).create(any(BlobInfo.class), any(byte[].class));
    }

    @Test
    void update_shouldThrowInvalidBucketPathException_whenFileDoesNotExist() {
        // Given
        String missingFile = "/path/to/local/missing.txt";

        // When / Then
        assertThrows(
                InvalidBucketPathException.class,
                () -> adapter.update(missingFile, "dir/file.txt"));
    }

    @Test
    void update_shouldThrowBucketOperationException_whenGcpFails() throws Exception {
        // Given
        File tempFile = File.createTempFile("test-file", ".txt");
        tempFile.deleteOnExit();

        doThrow(new RuntimeException("GCP failure"))
                .when(storage).create(any(BlobInfo.class), any(byte[].class));

        // When / Then
        BucketOperationException ex = assertThrows(
                BucketOperationException.class,
                () -> adapter.update(tempFile.getAbsolutePath(), "dir/file.txt"));

        assertTrue(ex.getMessage().contains("GCP error while uploading file"));
    }

    // ------------------------------------------------------------------
    // Delete method tests
    // ------------------------------------------------------------------

    @Test
    void delete_shouldDeleteSingleObject_whenRecursiveIsFalse() {
        // Given
        when(storage.delete(BUCKET, "dir/file.txt")).thenReturn(true);

        // When
        adapter.delete("dir/file.txt", false);

        // Then
        verify(storage).delete(BUCKET, "dir/file.txt");
    }

    @Test
    void delete_shouldDeleteAllObjects_whenRecursiveIsTrue() {
        // Given
        Blob blob1 = mock(Blob.class);
        Blob blob2 = mock(Blob.class);

        when(blob1.getBlobId()).thenReturn(BlobId.of(BUCKET, "dir/file1.txt"));
        when(blob2.getBlobId()).thenReturn(BlobId.of(BUCKET, "dir/sub/file2.txt"));

        @SuppressWarnings("unchecked")
        Page<Blob> page = mock(Page.class);
        when(page.iterateAll()).thenReturn(List.of(blob1, blob2));

        when(storage.list(eq(BUCKET), any(Storage.BlobListOption.class)))
                .thenReturn(page);

        // When
        adapter.delete("dir", true);

        // Then
        verify(storage).list(eq(BUCKET), any(Storage.BlobListOption.class));
        verify(storage).delete(
                List.of(
                        BlobId.of(BUCKET, "dir/file1.txt"),
                        BlobId.of(BUCKET, "dir/sub/file2.txt")));
    }

    @Test
    void delete_shouldThrowInvalidBucketPathException_whenRemoteSrcIsNull() {
        // When / Then
        assertThrows(
                InvalidBucketPathException.class,
                () -> adapter.delete(null, false));

        verifyNoInteractions(storage);
    }

    @Test
    void delete_shouldThrowInvalidBucketPathException_whenDeletingRoot() {
        // When / Then
        assertThrows(
                InvalidBucketPathException.class,
                () -> adapter.delete("/", false));

        verifyNoInteractions(storage);
    }

    @Test
    void delete_shouldThrowBucketObjectNotFoundException_whenObjectDoesNotExist_simple() {
        // Given
        when(storage.delete(BUCKET, "missing/file.txt")).thenReturn(false);

        // When / Then
        assertThrows(
                BucketObjectNotFoundException.class,
                () -> adapter.delete("missing/file.txt", false));

        verify(storage).delete(BUCKET, "missing/file.txt");
    }

    @Test
    void delete_shouldThrowBucketObjectNotFoundException_whenNoObjectsFound_recursive() {
        // Given
        @SuppressWarnings("unchecked")
        Page<Blob> emptyPage = mock(Page.class);
        when(emptyPage.iterateAll()).thenReturn(List.of());

        when(storage.list(eq(BUCKET), any(Storage.BlobListOption.class)))
                .thenReturn(emptyPage);

        // When / Then
        assertThrows(
                BucketObjectNotFoundException.class,
                () -> adapter.delete("empty", true));

        verify(storage).list(eq(BUCKET), any(Storage.BlobListOption.class));
    }

    @Test
    void delete_shouldThrowBucketOperationException_whenGcpDeleteFails() {
        // Given
        when(storage.delete(BUCKET, "dir/file.txt"))
                .thenThrow(new RuntimeException("GCP failure"));

        // When / Then
        assertThrows(
                BucketOperationException.class,
                () -> adapter.delete("dir/file.txt", false));
    }

    @Test
    void delete_shouldThrowBucketOperationException_whenGcpListFails() {
        // Given
        when(storage.list(eq(BUCKET), any(Storage.BlobListOption.class)))
                .thenThrow(new RuntimeException("GCP failure"));

        // When / Then
        assertThrows(
                BucketOperationException.class,
                () -> adapter.delete("dir", true));
    }

    // ------------------------------------------------------------------
    // List method tests
    // ------------------------------------------------------------------

    @Test
    void list_shouldReturnObjectNames_whenObjectsExist() {
        // Given
        when(blob1.getName()).thenReturn("file1.txt");
        when(blob2.getName()).thenReturn("dir/file2.txt");

        when(page.iterateAll()).thenReturn(List.of(blob1, blob2));
        when(storage.list(eq(BUCKET), any())).thenReturn(page);

        // When
        List<String> result = adapter.list("dir/");

        // Then
        assertNotNull(result);
        assertEquals(
                List.of("file1.txt", "dir/file2.txt"),
                result);

        verify(storage).list(eq(BUCKET), any());
    }

    @Test
    void list_shouldReturnEmptyList_whenNoObjectsFound() {
        // Given
        when(page.iterateAll()).thenReturn(List.of());
        when(storage.list(eq(BUCKET), any())).thenReturn(page);

        // When
        List<String> result = adapter.list("empty/");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(storage).list(eq(BUCKET), any());
    }

    @Test
    void list_shouldThrowBucketOperationException_whenGcpFails() {
        // Given
        when(storage.list(eq(BUCKET), any()))
                .thenThrow(new RuntimeException("GCP failure"));

        // When / Then
        BucketOperationException ex = assertThrows(
                BucketOperationException.class,
                () -> adapter.list("dir/"));

        assertTrue(
                ex.getMessage().contains("GCP error"));

        verify(storage).list(eq(BUCKET), any());
    }

    // ------------------------------------------------------------------
    // doesExists method tests
    // ------------------------------------------------------------------

    @Test
    void doesExists_shouldReturnTrue_whenObjectExists() {
        // Given
        Blob blob = mock(Blob.class);
        when(storage.get(BUCKET, "dir/file.txt")).thenReturn(blob);

        // When
        boolean exists = adapter.doesExists("dir/file.txt");

        // Then
        assertTrue(exists);
        verify(storage).get(BUCKET, "dir/file.txt");
    }

    @Test
    void doesExists_shouldReturnFalse_whenObjectDoesNotExist() {
        // Given
        when(storage.get(BUCKET, "dir/missing.txt")).thenReturn(null);

        // When
        boolean exists = adapter.doesExists("dir/missing.txt");

        // Then
        assertFalse(exists);
        verify(storage).get(BUCKET, "dir/missing.txt");
    }

    @Test
    void doesExists_shouldThrowInvalidBucketPathException_whenRemoteSrcIsNull() {
        // When / Then
        assertThrows(
                InvalidBucketPathException.class,
                () -> adapter.doesExists(null));

        verifyNoInteractions(storage);
    }

    @Test
    void doesExists_shouldThrowBucketOperationException_whenGcpFails() {
        // Given
        when(storage.get(BUCKET, "dir/file.txt"))
                .thenThrow(new RuntimeException("GCP failure"));

        // When / Then
        BucketOperationException ex = assertThrows(
                BucketOperationException.class,
                () -> adapter.doesExists("dir/file.txt"));

        assertTrue(ex.getMessage().contains("GCP error"));
        verify(storage).get(BUCKET, "dir/file.txt");
    }

    // ------------------------------------------------------------------
    // share method tests
    // ------------------------------------------------------------------

    @Test
    void share_shouldReturnSignedUrl_whenInputsAreValid() throws Exception {
        // Given
        String remoteSrc = "dir/file.txt";
        int expiration = 3600;

        URL signedUrl = URI.create(
                "https://storage.googleapis.com/test-bucket/dir/file.txt?X-Goog-Signature=abc").toURL();

        when(storage.signUrl(
                any(BlobInfo.class),
                eq((long) expiration),
                eq(TimeUnit.SECONDS),
                any(Storage.SignUrlOption.class))).thenReturn(signedUrl);

        // When
        String result = adapter.share(remoteSrc, expiration);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("X-Goog-Signature"));

        verify(storage).signUrl(
                any(BlobInfo.class),
                eq((long) expiration),
                eq(TimeUnit.SECONDS),
                any(Storage.SignUrlOption.class));
    }

    @Test
    void share_shouldThrowInvalidBucketPathException_whenRemoteSrcIsNull() {
        // When / Then
        assertThrows(
                InvalidBucketPathException.class,
                () -> adapter.share(null, 3600));

        verifyNoInteractions(storage);
    }

    @Test
    void share_shouldThrowInvalidBucketPathException_whenExpirationIsInvalid() {
        // When / Then
        assertThrows(
                InvalidBucketPathException.class,
                () -> adapter.share("dir/file.txt", 0));

        verifyNoInteractions(storage);
    }

    @Test
    void share_shouldThrowBucketOperationException_whenGcpFails() {
        // Given
        when(storage.signUrl(
                any(BlobInfo.class),
                anyLong(),
                any(TimeUnit.class),
                any(Storage.SignUrlOption.class))).thenThrow(new RuntimeException("GCP failure"));

        // When / Then
        BucketOperationException ex = assertThrows(
                BucketOperationException.class,
                () -> adapter.share("dir/file.txt", 3600));

        assertTrue(ex.getMessage().contains("GCP error"));

        verify(storage).signUrl(
                any(BlobInfo.class),
                anyLong(),
                any(TimeUnit.class),
                any(Storage.SignUrlOption.class));
    }
}