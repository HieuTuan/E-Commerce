package com.mypkga.commerceplatformfull.config;

import com.mypkga.commerceplatformfull.service.FileService;
import com.mypkga.commerceplatformfull.service.impl.LocalFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
public class FileStorageConfig {
    
    @Value("${app.file-storage.type:local}")
    private String storageType;
    
    @Bean
    @ConditionalOnProperty(name = "app.file-storage.type", havingValue = "local", matchIfMissing = true)
    public FileService localFileService() {
        log.info("Configuring local file storage service");
        return new LocalFileService();
    }
}