package com.example.bucketAdapter.factories;

import org.springframework.stereotype.Component;
import com.example.bucketAdapter.models.CloudProvider;
import com.example.bucketAdapter.adapters.BucketAdapter;
import com.example.bucketAdapter.adapters.impl.AwsAdapterImpl;
import com.example.bucketAdapter.adapters.impl.AzureAdapterImpl;
import com.example.bucketAdapter.adapters.impl.GcpAdapterImpl;

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
