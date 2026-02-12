package com.example.bucketadapter.factory;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.bucketadapter.adapter.BucketAdapter;

@Component
public class BucketAdapterFactory {

    //TODO NGY Avoid DI by Field Injection. Prefer Injection by method and configuration
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
