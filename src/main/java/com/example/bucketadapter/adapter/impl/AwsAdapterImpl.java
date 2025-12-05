package com.example.bucketadapter.adapter.impl;

import org.springframework.stereotype.Component;

import com.example.bucketadapter.adapter.BucketAdapter;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AwsAdapterImpl implements BucketAdapter {

    private final S3Client s3Client;
    private final String bucket = "my-bucket";

    public AwsAdapterImpl() {
        this.s3Client = S3Client.builder()
                .region(Region.EU_WEST_1)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }

    @Override
    public void upload(String localSrc, String remoteSrc) {
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(remoteSrc)
                .build(),
                RequestBody.fromFile(new File(localSrc)));
    }

    @Override
    public void download(String localSrc, String remoteSrc) {
        s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(remoteSrc)
                .build(),
                Paths.get(localSrc));
    }

    @Override
    public void delete(String remoteSrc, boolean recursive) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(remoteSrc)
                .build());
    }

    @Override
    public List<String> list(String remoteSrc) {
        ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(remoteSrc)
                .build());

        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }
}
