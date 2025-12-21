package com.example.bucketadapter.adapter.impl;

import org.springframework.stereotype.Component;

import com.example.bucketadapter.adapter.BucketAdapter;
import com.example.bucketadapter.provider.AZURESDK;

import java.util.List;

@Component("AZURE")
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
    public void update(String localSrc, String remoteSrc) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public void delete(String remoteSrc, boolean recursive) {
        azureSdk.deleteObject(remoteSrc);
    }

    @Override
    public List<String> list(String remoteSrc) {
        return azureSdk.listObjects(remoteSrc);
    }

    @Override
    public boolean doesExists(String remoteSrc) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doesExists'");
    }

    @Override
    public String share(String remoteSrc, int expirationTime) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'share'");
    }
}
