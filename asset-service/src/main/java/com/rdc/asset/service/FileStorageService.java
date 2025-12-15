package com.rdc.asset.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {
    // store file and return stored filename (or key)
    String store(MultipartFile file) throws IOException;

    Path load(String filename);

    byte[] readAllBytes(String filename) throws IOException;

    void delete(String filename) throws IOException;
}
