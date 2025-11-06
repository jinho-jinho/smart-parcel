package com.capstone.smart_parcel.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;

@Configuration
@RequiredArgsConstructor
public class StaticResourceConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @PostConstruct
    void ensureStorageDirectory() throws IOException {
        Files.createDirectories(storageProperties.imageDirPath());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(storageProperties.publicUrlPattern())
                .addResourceLocations(storageProperties.imageDirLocation())
                .setCachePeriod(3600)
                .resourceChain(true);
    }
}
