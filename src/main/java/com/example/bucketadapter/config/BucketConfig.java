package com.example.bucketadapter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.bucketadapter.adapter.BucketAdapter;
import com.example.bucketadapter.factory.BucketAdapterFactory;
import com.example.bucketadapter.model.CloudProvider;
import com.example.bucketadapter.service.BucketService;

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
