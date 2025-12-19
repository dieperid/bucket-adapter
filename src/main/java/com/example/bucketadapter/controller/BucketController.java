package com.example.bucketadapter.controller;

import org.springframework.web.bind.annotation.*;

import com.example.bucketadapter.service.BucketService;

import java.util.*;

@RestController
@RequestMapping("/api")
public class BucketController {

    private final BucketService bucketService;

    public BucketController(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    /*
     * CURL sample :
     * 
     * curl -s "http://localhost:8080/api/list?path="
     */
    @GetMapping("/list")
    public List<String> list(@RequestParam String path) {
        return bucketService.list(path);
    }
}
