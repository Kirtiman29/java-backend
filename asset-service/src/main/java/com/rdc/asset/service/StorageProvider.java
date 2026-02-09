package com.rdc.asset.service;

import java.io.InputStream;
import java.io.IOException;

public interface StorageProvider {

    void write(String relativePath, InputStream data) throws IOException;

    InputStream read(String relativePath) throws IOException;

    void delete(String relativePath);
}