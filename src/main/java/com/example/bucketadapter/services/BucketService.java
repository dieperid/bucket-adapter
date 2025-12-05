package com.example.bucketadapter.services;

import org.springframework.stereotype.Service;
import com.example.bucketadapter.adapters.BucketAdapter;
import java.util.List;

@Service
public class BucketService {

    private final BucketAdapter adapter;

    public BucketService(BucketAdapter adapter) {
        this.adapter = adapter;
    }

    public void upload(String local, String remote) {
        adapter.upload(local, remote);
    }

    public void download(String local, String remote) {
        adapter.download(local, remote);
    }

    public void delete(String remote, boolean recursive) {
        adapter.delete(remote, recursive);
    }

    public List<String> list(String remote) {
        return adapter.list(remote);
    }
}
