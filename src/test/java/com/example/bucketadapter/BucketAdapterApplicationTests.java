package com.example.bucketadapter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.bucketadapter.adapter.BucketAdapter;
import com.example.bucketadapter.factory.BucketAdapterFactory;

@SpringBootTest
@ActiveProfiles("test")
class BucketAdapterApplicationTests {

    @MockitoBean
    private BucketAdapterFactory factory;

    @BeforeEach
    void setup() {
        when(factory.getAdapter()).thenReturn(mock(BucketAdapter.class));
    }

    @Test
    void contextLoads() {
    }

}
