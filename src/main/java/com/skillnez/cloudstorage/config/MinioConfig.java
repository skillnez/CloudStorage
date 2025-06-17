package com.skillnez.cloudstorage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.minio.MinioClient;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${minio.url}")
            String url,
            @Value("${minio.access-key}")
            String minioAccessKey,
            @Value("${minio.secret-key}")
            String minioSecretKey) {

        return MinioClient.builder()
                .endpoint(url)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

}
