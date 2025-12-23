package com.example.bucketadapter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.example.bucketadapter.config.DotenvInitializer;

@SpringBootApplication
public class BucketAdapterApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(BucketAdapterApplication.class)
                .initializers(new DotenvInitializer())
                .run(args);
    }

}
