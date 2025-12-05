package com.example.bucketadapter.provider;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AWSSDK {
    public void putObject(String src, String dest) {
    }

    public void getObject(String src, String dest) {
    }

    public void deleteObject(String src) {
    }

    public List<String> listObjects(String src) {
        return List.of("file1.txt", "file2.txt", "folder/file3.txt");
    }
}
