package com.rdc.admin.controller;

import com.rdc.admin.dto.BlogCreateRequest;
import com.rdc.admin.dto.BlogResponse;
import com.rdc.admin.dto.BlogUpdateRequest;
import com.rdc.admin.service.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @PostMapping("/api/admin/blogs")
    public ResponseEntity<BlogResponse> create(@Valid @RequestBody BlogCreateRequest request) {
        return new ResponseEntity<>(blogService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/api/admin/blogs/{id}")
    public ResponseEntity<BlogResponse> update(@PathVariable Long id, @RequestBody BlogUpdateRequest request) {
        return ResponseEntity.ok(blogService.update(id, request));
    }

    @GetMapping("/api/admin/blogs")
    public ResponseEntity<List<BlogResponse>> getAllAdmin() {
        return ResponseEntity.ok(blogService.getAllAdmin());
    }

    @GetMapping("/api/admin/blogs/{id}")
    public ResponseEntity<BlogResponse> getAdminById(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.getAdminById(id));
    }

    @DeleteMapping("/api/admin/blogs/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        blogService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/public/blogs")
    public ResponseEntity<List<BlogResponse>> getPublished() {
        return ResponseEntity.ok(blogService.getPublished());
    }

    @GetMapping("/api/public/blogs/featured")
    public ResponseEntity<List<BlogResponse>> getFeatured() {
        return ResponseEntity.ok(blogService.getFeatured());
    }

    @GetMapping("/api/public/blogs/{slug}")
    public ResponseEntity<BlogResponse> getPublishedBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(blogService.getPublishedBySlug(slug));
    }
}
