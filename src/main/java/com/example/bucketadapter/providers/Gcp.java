package com.example.bucketadapter.providers;

import org.springframework.stereotype.Component;

@Component
public class GCPSDK {
    public void putObject(String src, String dest) {}
    public void getObject(String src, String dest) {}
    public void deleteObject(String src) {}
    public List<String> listObjects(String src) { return List.of(); }
}
