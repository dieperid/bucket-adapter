package com.example.bucketadapter.adapter.impl;

import org.springframework.stereotype.Component;

import com.example.bucketadapter.adapter.BucketAdapter;
import com.example.bucketadapter.provider.GCPSDK;

import java.util.List;

@Component
public class GcpAdapterImpl implements BucketAdapter {

    private final GCPSDK gcpSdk;

    public GcpAdapterImpl(GCPSDK gcpSdk) {
        this.gcpSdk = gcpSdk;
    }

    @Override
    public void upload(String localSrc, String remoteSrc) {
        gcpSdk.putObject(localSrc, remoteSrc);
    }

    @Override
    public void download(String localSrc, String remoteSrc) {
        gcpSdk.getObject(localSrc, remoteSrc);
    }

    @Override
    public void delete(String remoteSrc, boolean recursive) {
        gcpSdk.deleteObject(remoteSrc);
    }

    @Override
    public List<String> list(String remoteSrc) {
        return gcpSdk.listObjects(remoteSrc);
    }
}
