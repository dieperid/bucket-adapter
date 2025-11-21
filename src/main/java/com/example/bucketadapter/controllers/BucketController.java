package com.example.bucketadapter.controllers;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/bucket")
public class BucketController {

    private final BucketService bucketService;

    public BucketController(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    @GetMapping("/list")
    public List<String> list(@RequestParam String path) {
        return bucketService.list(path);
    }
}
