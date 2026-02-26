package com.example.bucketadapter.exception;

public class BucketOperationException extends RuntimeException {
    public BucketOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
