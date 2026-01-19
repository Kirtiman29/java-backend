package com.rdc.asset.service;

import java.io.InputStream;
import java.io.IOException;

public interface StorageProvider {
    void write(String path, InputStream data) throws IOException;
    InputStream read(String path) throws IOException;
    void delete(String path) throws IOException;
}