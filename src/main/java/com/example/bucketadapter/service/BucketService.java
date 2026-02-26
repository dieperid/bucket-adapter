package com.example.bucketadapter.service;

import org.springframework.stereotype.Service;

import com.example.bucketadapter.adapter.BucketAdapter;
import com.example.bucketadapter.factory.BucketAdapterFactory;

import jakarta.annotation.PostConstruct;

import java.util.List;

@Service
public class BucketService {

    private final BucketAdapterFactory factory;
    private BucketAdapter adapter;

    public BucketService(BucketAdapterFactory factory) {
        this.factory = factory;
    }

    @PostConstruct
    void init() {
        this.adapter = factory.getAdapter();
    }

    public void upload(String remote, byte[] content) {
        adapter.upload(remote, content);
    }

    public byte[] download(String remote) {
        return adapter.download(remote);
    }

    public void update(String remote, byte[] content) {
        adapter.update(remote, content);
    }

    public void delete(String remote, boolean recursive) {
        adapter.delete(remote, recursive);
    }

    public List<String> list(String remote) {
        return adapter.list(remote);
    }

    public boolean doesExists(String remote) {
        return adapter.doesExists(remote);
    }

    public String share(String remote, int expirationTime) {
        return adapter.share(remote, expirationTime);
    }
}
