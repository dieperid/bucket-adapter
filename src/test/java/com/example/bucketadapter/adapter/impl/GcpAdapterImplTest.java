package com.example.bucketadapter.adapter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bucketadapter.exception.BucketOperationException;
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
}