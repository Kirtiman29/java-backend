package com.rdc.asset.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.location:./data/uploads}")
    private String storageLocation;

    private Path root;

    @PostConstruct
    public void init() throws IOException {
        this.root = Paths.get(storageLocation).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        String original = Path.of(file.getOriginalFilename()).getFileName().toString();
        String filename = System.currentTimeMillis() + "-" + java.util.UUID.randomUUID() + "-" + original;
        Path target = root.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    @Override
    public Path load(String filename) {
        return root.resolve(filename).normalize();
    }

    @Override
    public byte[] readAllBytes(String filename) throws IOException {
        return Files.readAllBytes(load(filename));
    }

    @Override
    public void delete(String filename) throws IOException {
        Files.deleteIfExists(load(filename));
    }
}
