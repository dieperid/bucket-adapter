package com.example.bucketadapter.adapter;

import java.util.List;

public interface BucketAdapter {
    void upload(String localSrc, String remoteSrc);

    void download(String localSrc, String remoteSrc);

    void delete(String remoteSrc, boolean recursive);

    List<String> list(String remoteSrc);

    boolean doesExists(String remoteSrc);

    String share(String remoteSrc, int expirationTime);
}
