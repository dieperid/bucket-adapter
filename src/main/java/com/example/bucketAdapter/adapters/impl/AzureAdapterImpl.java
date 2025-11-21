package com.example.bucketAdapter.adapters.impl;

import org.springframework.stereotype.Component;
import com.example.bucketAdapter.adapters.BucketAdapter;
import com.example.bucketAdapter.providers.AZURESDK;
import java.util.List;

@Component
public class AzureAdapterImpl implements BucketAdapter {

    private final AZURESDK azureSdk;

    public AzureAdapterImpl(AZURESDK azureSdk) {
        this.azureSdk = azureSdk;
    }

    @Override
    public void upload(String localSrc, String remoteSrc) {
        azureSdk.putObject(localSrc, remoteSrc);
    }

    @Override
    public void download(String localSrc, String remoteSrc) {
        azureSdk.getObject(localSrc, remoteSrc);
    }

    @Override
    public void delete(String remoteSrc, boolean recursive) {
        azureSdk.deleteObject(remoteSrc);
    }

    @Override
    public List<String> list(String remoteSrc) {
        return azureSdk.listObjects(remoteSrc);
    }
}
