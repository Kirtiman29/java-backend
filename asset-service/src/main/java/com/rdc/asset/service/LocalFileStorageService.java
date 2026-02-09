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

    /**
     * Persists the physical file to the local disk.
     */
    @Override
    public void write(String path, InputStream data) throws IOException {
        Path filePath = Paths.get(storageLocation).resolve(path);
        Files.createDirectories(filePath.getParent());
        Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("💾 File written: {}", filePath);
    }

    /**
     * Reads the physical file from the local disk as an InputStream.
     */
    @Override
    public InputStream read(String path) throws IOException {
        Path filePath = Paths.get(storageLocation).resolve(path);
        return new FileInputStream(filePath.toFile());
    }

    /**
     * ✅ UPDATED: Robust physical file deletion.
     * Gracefully handles cases where the file might already be missing to ensure
     * database transactions can still complete.
     */
    @Override
    public void delete(String relativePath) {
        try {
            // Resolve relative database path to absolute disk path
            Path path = Paths.get(storageLocation).resolve(relativePath);

            if (!Files.exists(path)) {
                // ❗ Log warning but do not throw exception to allow DB cleanup to proceed
                log.warn("⚠️ Physical file already missing from storage: {}", path);
                return;
            }

            // Perform the physical deletion from the disk
            Files.delete(path);
            log.info("🗑️ Successfully deleted physical file: {}", path);

        } catch (IOException e) {
            log.error("❌ Fatal error during physical file deletion for: {}", relativePath);
            // Throwing here indicates a system/permission error rather than a missing file
            throw new RuntimeException("Failed to delete physical file: " + relativePath, e);
        }
    }
}