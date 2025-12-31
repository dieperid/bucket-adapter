package com.example.bucketadapter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.bucketadapter.service.BucketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Bucket API", description = "API to manage files stored in a bucket (upload, download, update, delete, list, share)")
public class BucketController {

    private final BucketService bucketService;

    public BucketController(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    /**
     * Upload a file to the bucket.
     * 
     * CURL sample :
     * curl -X POST
     * "http://localhost:8080/api/files?localPath=/path/to/local/file.txt&remoteSrc=/path/in/bucket/file.txt"
     * 
     * @param localPath - path to the local file
     * @param remoteSrc - path in the bucket
     */
    @Operation(summary = "Upload a file", description = "Uploads a local file to the bucket at the specified remote path")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/files")
    @ResponseStatus(HttpStatus.CREATED)
    public void upload(
            @Parameter(description = "Path to the local file to upload", example = "/path/to/local/file.txt", required = true) @RequestParam String localPath,
            @Parameter(description = "Destination path in the bucket", example = "/path/in/bucket/file.txt", required = true) @RequestParam String remoteSrc) {
        bucketService.upload(localPath, remoteSrc);
    }

    /**
     * Download a file from the bucket.
     * 
     * CURL sample :
     * curl -X GET
     * "http://localhost:8080/api/files/download?localPath=/path/to/local/file.txt&remoteSrc=/path/in/bucket/file.txt"
     * 
     * @param localPath - path to the local file
     * @param remoteSrc - path in the bucket
     */
    @Operation(summary = "Download a file", description = "Downloads a file from the bucket to a local destination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping(value = "/files/download", params = "remoteSrc")
    @ResponseStatus(HttpStatus.OK)
    public void download(
            @Parameter(description = "Local path where the file will be saved", example = "/path/to/local/file.txt", required = true) @RequestParam String localPath,
            @Parameter(description = "Path of the file in the bucket", example = "/path/in/bucket/file.txt", required = true) @RequestParam String remoteSrc) {
        bucketService.download(localPath, remoteSrc);
    }

    /**
     * Update a file in the bucket.
     * 
     * CURL sample :
     * curl -X PUT
     * "http://localhost:8080/api/files?localPath=/path/to/local/file.txt&remoteSrc=/path/in/bucket/file.txt"
     * 
     * @param localPath - path to the local file
     * @param remoteSrc - path in the bucket
     */
    @Operation(summary = "Update a file", description = "Replaces an existing file in the bucket with a new local file")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "File updated successfully"),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PutMapping(value = "/files", params = "remoteSrc")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @Parameter(description = "Path to the local file", example = "/path/to/local/file.txt", required = true) @RequestParam String localPath,
            @Parameter(description = "Path of the file to update in the bucket", example = "/path/in/bucket/file.txt", required = true) @RequestParam String remoteSrc) {
        bucketService.update(localPath, remoteSrc);
    }

    /**
     * Delete a file from the bucket.
     * 
     * CURL sample :
     * curl -X DELETE
     * "http://localhost:8080/api/files?remoteSrc=/path/in/bucket/file.txt&recursive=false"
     * 
     * @param remoteSrc - path in the bucket
     * @param recursive - whether to delete recursively
     */
    @Operation(summary = "Delete a file or directory", description = "Deletes a file or directory from the bucket, optionally recursively")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deletion completed successfully"),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping(value = "/files", params = "remoteSrc")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Path of the file or directory in the bucket", example = "/path/in/bucket/file.txt", required = true) @RequestParam String remoteSrc,
            @Parameter(description = "Whether the deletion should be recursive (for directories)", example = "false") @RequestParam(defaultValue = "false") boolean recursive) {
        bucketService.delete(remoteSrc, recursive);
    }

    /**
     * List all files in a directory.
     * 
     * CURL sample :
     * curl -X GET "http://localhost:8080/api/files?path="
     * 
     * @param path - directory path in the bucket
     * @return - list of file paths
     */
    @Operation(summary = "List files in a directory", description = "Returns a list of files located in a directory within the bucket")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of files returned successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping(value = "/files", params = "path")
    public List<String> list(
            @Parameter(description = "Directory path in the bucket", example = "/path/in/bucket", required = true) @RequestParam String path) {
        return bucketService.list(path);
    }

    /**
     * Share a file from the bucket.
     * 
     * CURL sample :
     * curl -X POST
     * "http://localhost:8080/api/files/share?remoteSrc=/path/in/bucket/file.txt&expirationTime=3600"
     * 
     * @param remoteSrc      - path in the bucket
     * @param expirationTime - expiration time in seconds
     * @return - shared URL
     */
    @Operation(summary = "Share a file", description = "Generates a temporary public URL to access a file stored in the bucket")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shared URL generated successfully"),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/files/share")
    public String share(
            @Parameter(description = "Path of the file in the bucket", example = "/path/in/bucket/file.txt", required = true) @RequestParam String remoteSrc,
            @Parameter(description = "Expiration time in seconds", example = "3600") @RequestParam int expirationTime) {
        return bucketService.share(remoteSrc, expirationTime);
    }
}
