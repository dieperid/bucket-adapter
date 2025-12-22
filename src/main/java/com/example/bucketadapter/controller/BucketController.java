package com.example.bucketadapter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * Upload a file to the bucket.
     * 
     * CURL sample :
     * curl -s
     * "http://localhost:8080/api/files?localPath=/path/to/local/file.txt&remoteSrc=/path/in/bucket/file.txt"
     * 
     * @param localPath - path to the local file
     * @param remoteSrc - path in the bucket
     */
    @PostMapping("/files")
    @ResponseStatus(HttpStatus.CREATED)
    public void upload(@RequestParam String localPath, @RequestParam String remoteSrc) {
        bucketService.upload(localPath, remoteSrc);
    }

    /**
     * Download a file from the bucket.
     * 
     * CURL sample :
     * curl -s
     * "http://localhost:8080/api/files/download?localPath=/path/to/local/file.txt&remoteSrc=/path/in/bucket/file.txt"
     * 
     * @param localPath - path to the local file
     * @param remoteSrc - path in the bucket
     */
    @GetMapping(value = "/files/download", params = "remoteSrc")
    @ResponseStatus(HttpStatus.OK)
    public void download(@RequestParam String localPath, @RequestParam String remoteSrc) {
        bucketService.download(localPath, remoteSrc);
    }

    /**
     * Update a file in the bucket.
     * 
     * CURL sample :
     * curl -s
     * "http://localhost:8080/api/files?localPath=/path/to/local/file.txt&remoteSrc=/path/in/bucket/file.txt"
     * 
     * @param localPath - path to the local file
     * @param remoteSrc - path in the bucket
     */
    @PutMapping(value = "/files", params = "remoteSrc")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@RequestParam String localPath, @RequestParam String remoteSrc) {
        bucketService.update(localPath, remoteSrc);
    }

    /**
     * Delete a file from the bucket.
     * 
     * CURL sample :
     * curl -s
     * "http://localhost:8080/api/files?remoteSrc=/path/in/bucket/file.txt&recursive=false"
     * 
     * @param remoteSrc - path in the bucket
     * @param recursive - whether to delete recursively
     */
    @DeleteMapping(value = "/files", params = "remoteSrc")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String remoteSrc, @RequestParam(defaultValue = "false") boolean recursive) {
        bucketService.delete(remoteSrc, recursive);
    }

    /**
     * List all files in a directory.
     * 
     * CURL sample :
     * curl -s "http://localhost:8080/api/files?path="
     * 
     * @param path - directory path in the bucket
     * @return - list of file paths
     */
    @GetMapping(value = "/files", params = "path")
    public List<String> list(@RequestParam String path) {
        return bucketService.list(path);
    }

    /**
     * Check if a file exists in the bucket.
     * 
     * CURL sample :
     * curl -s
     * "http://localhost:8080/api/files/exists?remoteSrc=/path/in/bucket/file.txt"
     *
     * @param remoteSrc - path in the bucket
     * @return - true if the file exists, false otherwise
     */
    @GetMapping(value = "/files/exists", params = "remoteSrc")
    public ResponseEntity<Void> doesExists(@RequestParam String remoteSrc) {
        return bucketService.doesExists(remoteSrc)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Share a file from the bucket.
     * 
     * CURL sample :
     * curl -s
     * "http://localhost:8080/api/files/share?remoteSrc=/path/in/bucket/file.txt&expirationTime=3600"
     * 
     * @param remoteSrc      - path in the bucket
     * @param expirationTime - expiration time in seconds
     * @return - shared URL
     */
    @PostMapping("/files/share")
    public String share(@RequestParam String remoteSrc, @RequestParam int expirationTime) {
        return bucketService.share(remoteSrc, expirationTime);
    }
}
