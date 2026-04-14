package com.rdc.asset.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocalFileStorageService implements StorageProvider {

    @Value("${app.storage.location:./data/uploads}")
    private String storageLocation;

    @Override
    public void write(String path, InputStream data) throws IOException {
        Path filePath = Paths.get(storageLocation).resolve(path);
        Files.createDirectories(filePath.getParent());
        Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("File written: {}", filePath);
    }

    @Override
    public InputStream read(String path) throws IOException {
        Path filePath = Paths.get(storageLocation).resolve(path);
        return new FileInputStream(filePath.toFile());
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path path = Paths.get(storageLocation).resolve(relativePath);

            if (!Files.exists(path)) {
                log.warn("Physical file already missing from storage: {}", path);
                return;
            }

            Files.delete(path);
            log.info("Successfully deleted physical file: {}", path);

        } catch (IOException e) {
            log.error("Fatal error during physical file deletion for: {}", relativePath);
            throw new RuntimeException("Failed to delete physical file: " + relativePath, e);
        }
    }
}