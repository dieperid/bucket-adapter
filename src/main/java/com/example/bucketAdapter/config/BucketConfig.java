package com.example.bucketAdapter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.bucketAdapter.factories.BucketAdapterFactory;
import com.example.bucketAdapter.models.CloudProvider;
import com.example.bucketAdapter.adapter.BucketAdapter;
import com.example.bucketAdapter.services.BucketService;

@Configuration
public class BucketConfig {

    @Value("${bucket.provider:AWS}")
    private String providerName;

    @Bean
    public BucketService bucketService(BucketAdapterFactory factory) {
        CloudProvider provider = CloudProvider.valueOf(providerName.toUpperCase());
        BucketAdapter adapter = factory.getAdapter(provider);
        return new BucketService(adapter);
    }
}
