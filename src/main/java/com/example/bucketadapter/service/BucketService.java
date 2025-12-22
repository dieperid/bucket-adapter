package com.example.bucketadapter.service;

import org.springframework.stereotype.Service;

import com.example.bucketadapter.adapter.BucketAdapter;
import com.example.bucketadapter.factory.BucketAdapterFactory;

import java.util.List;

@Service
public class BucketService {

    private final BucketAdapter adapter;

    public BucketService(BucketAdapterFactory factory) {
        this.adapter = factory.getAdapter();
    }

    public void upload(String local, String remote) {
        adapter.upload(local, remote);
    }

    public void download(String local, String remote) {
        adapter.download(local, remote);
    }

    public void update(String local, String remote) {
        adapter.update(local, remote);
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
