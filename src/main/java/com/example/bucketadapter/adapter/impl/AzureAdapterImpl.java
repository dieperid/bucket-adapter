package com.example.bucketadapter.adapter.impl;

import org.springframework.stereotype.Component;

import com.example.bucketadapter.adapter.BucketAdapter;

import java.util.List;

@Component("AZURE")
public class AzureAdapterImpl implements BucketAdapter {

    @Override
    public void upload(String localSrc, String remoteSrc) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'upload'");
    }

    @Override
    public void download(String localSrc, String remoteSrc) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'download'");
    }

    @Override
    public void update(String localSrc, String remoteSrc) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public void delete(String remoteSrc, boolean recursive) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<String> list(String remoteSrc) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'list'");
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
