package com.example.bucketadapter.factory;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.example.bucketadapter.adapter.BucketAdapter;

@Component
public class BucketAdapterFactory {

    private final Map<String, BucketAdapter> adapters;

    public BucketAdapterFactory(Map<String, BucketAdapter> adapters) {
        this.adapters = adapters;
    }

    public BucketAdapter getAdapter() {
        String provider = System.getProperty("PROVIDER_IMPL");

        BucketAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        return adapter;
    }
}
