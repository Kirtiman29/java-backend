package com.rdc.asset.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;

@Service
public class FileStorageService {

    @Value("${app.storage.location:./data/uploads}")
    private String storageLocation;

    public String store(MultipartFile file) throws IOException {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(storageLocation).resolve(filename);
        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    // FIX: Added missing method for vault streaming
    public InputStream getInputStream(String filename) throws IOException {
        Path filePath = Paths.get(storageLocation).resolve(filename);
        return new FileInputStream(filePath.toFile());
    }
}