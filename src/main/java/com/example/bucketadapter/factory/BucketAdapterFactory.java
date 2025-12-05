package com.example.bucketadapter.factory;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.example.bucketadapter.adapter.BucketAdapter;
import com.example.bucketadapter.adapter.impl.AwsAdapterImpl;
import com.example.bucketadapter.adapter.impl.AzureAdapterImpl;
import com.example.bucketadapter.adapter.impl.GcpAdapterImpl;
import com.example.bucketadapter.model.CloudProvider;

@Component
public class BucketAdapterFactory {

    private final ApplicationContext context;

    public BucketAdapterFactory(ApplicationContext context) {
        this.context = context;
    }

    public BucketAdapter getAdapter(CloudProvider provider) {
        return switch (provider) {
            case AWS -> context.getBean(AwsAdapterImpl.class);
            case AZURE -> context.getBean(AzureAdapterImpl.class);
            case GCP -> context.getBean(GcpAdapterImpl.class);
        };
    }
}
