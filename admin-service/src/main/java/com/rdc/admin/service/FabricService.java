package com.rdc.admin.service;

import com.rdc.admin.dto.BulkUploadResponse;
import com.rdc.admin.dto.FabricCreateRequest;
import com.rdc.admin.dto.FabricResponse;
import com.rdc.admin.dto.FabricUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface FabricService {
    FabricResponse create(FabricCreateRequest req);
    FabricResponse update(Long id, FabricUpdateRequest req);
    List<FabricResponse> getAll();
    List<FabricResponse> getAllAdmin();
    FabricResponse getById(Long id);
    void delete(Long id);
    void updateStock(Long id, Double meters);
    BulkUploadResponse processBulk(InputStream csvStream, MultipartFile[] assets);
}
