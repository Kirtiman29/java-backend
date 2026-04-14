package com.rdc.admin.service;

import com.rdc.admin.dto.BlogCreateRequest;
import com.rdc.admin.dto.BlogResponse;
import com.rdc.admin.dto.BlogUpdateRequest;

import java.util.List;

public interface BlogService {

    BlogResponse create(BlogCreateRequest request);

    BlogResponse update(Long id, BlogUpdateRequest request);

    List<BlogResponse> getAllAdmin();

    List<BlogResponse> getPublished();

    List<BlogResponse> getFeatured();

    BlogResponse getAdminById(Long id);

    BlogResponse getPublishedBySlug(String slug);

    void delete(Long id);
}
