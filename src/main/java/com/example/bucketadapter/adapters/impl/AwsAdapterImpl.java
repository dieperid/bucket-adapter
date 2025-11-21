package com.example.bucketadapter.adapters.impl;

import org.springframework.stereotype.Component;
import com.example.bucketadapter.adapters.BucketAdapter;
import com.example.bucketadapter.providers.AWSSDK;
import java.util.List;

@Component
public class AwsAdapterImpl implements BucketAdapter {

    private final AWSSDK awsSDK;

    public AwsAdapterImpl(AWSSDK awsSDK) {
        this.awsSDK = awsSDK;
    }

    @Override
    public void upload(String localSrc, String remoteSrc) {
        awsSDK.putObject(localSrc, remoteSrc);
    }

    @Override
    public void download(String localSrc, String remoteSrc) {
        awsSDK.getObject(localSrc, remoteSrc);
    }

    @Override
    public void delete(String remoteSrc, boolean recursive) {
        awsSDK.deleteObject(remoteSrc);
    }

    @Override
    public List<String> list(String remoteSrc) {
        return awsSDK.listObjects(remoteSrc);
    }
}
