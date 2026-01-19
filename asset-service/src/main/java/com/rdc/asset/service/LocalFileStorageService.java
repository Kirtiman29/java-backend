package com.rdc.asset.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;

@Service
public class LocalFileStorageService implements StorageProvider {

    @Value("${app.storage.location:./data/uploads}")
    private String storageLocation;

    @Override
    public void write(String path, InputStream data) throws IOException {
        Path filePath = Paths.get(storageLocation).resolve(path);
        Files.createDirectories(filePath.getParent());
        Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream read(String path) throws IOException {
        Path filePath = Paths.get(storageLocation).resolve(path);
        return new FileInputStream(filePath.toFile());
    }

    @Override
    public void delete(String path) throws IOException {
        Path filePath = Paths.get(storageLocation).resolve(path);
        Files.deleteIfExists(filePath);
    }
}