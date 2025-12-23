package com.example.bucketadapter.factory;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.bucketadapter.adapter.BucketAdapter;

@Component
public class BucketAdapterFactory {

    @Autowired
    private Map<String, BucketAdapter> adapters;

    public BucketAdapter getAdapter() {
        String provider = System.getProperty("PROVIDER_IMPL");

        BucketAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        return adapter;
    }
}
