package com.example.bucketadapter.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {

    private static final Logger log = LoggerFactory.getLogger(EnvConfig.class);

    @PostConstruct
    public void loadEnv() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // Don't crash if .env is missing in production
                .load();

        // Copy ALL variables from .env to System properties
        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue();

            // Set as system property (supports both naming conventions)
            System.setProperty(key, value); // Original name (AWS_S3_BUCKET_NAME)

            log.debug("Loaded env variable: {} -> {}", key, maskedValue(value, key));
        });

        log.info("Loaded {} environment variables from .env", dotenv.entries().size());
    }

    private String maskedValue(String value, String key) {
        if (key.contains("SECRET") || key.contains("KEY") || key.contains("PASSWORD") || key.contains("CREDENTIALS")) {
            if (value.length() > 8) {
                return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
            }
            return "***";
        }
        return value;
    }
}