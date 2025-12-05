package com.example.bucketadapter.factories;

import org.springframework.stereotype.Component;
import com.example.bucketadapter.models.CloudProvider;
import com.example.bucketadapter.adapters.BucketAdapter;
import com.example.bucketadapter.adapters.impl.AwsAdapterImpl;
import com.example.bucketadapter.adapters.impl.AzureAdapterImpl;
import com.example.bucketadapter.adapters.impl.GcpAdapterImpl;

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
