package com.example.bucketadapter.factories;

import org.springframework.stereotype.Component;
import com.example.bucketadapter.models.CloudProvider;
import com.example.bucketadapter.adapters.BucketAdapter;
import com.example.bucketadapter.adapters.impl.AwsAdapterImpl;
import com.example.bucketadapter.adapters.impl.AzureAdapterImpl;
import com.example.bucketadapter.adapters.impl.GcpAdapterImpl;

@Component
public class BucketAdapterFactory {

    private final AwsAdapterImpl awsAdapter;
    private final AzureAdapterImpl azureAdapter;
    private final GcpAdapterImpl gcpAdapter;

    public BucketAdapterFactory(AwsAdapterImpl awsAdapter, AzureAdapterImpl azureAdapter, GcpAdapterImpl gcpAdapter) {
        this.awsAdapter = awsAdapter;
        this.azureAdapter = azureAdapter;
        this.gcpAdapter = gcpAdapter;
    }

    public BucketAdapter getAdapter(CloudProvider provider) {
        return switch (provider) {
            case AWS -> awsAdapter;
            case AZURE -> azureAdapter;
            case GCP -> gcpAdapter;
        };
    }
}
