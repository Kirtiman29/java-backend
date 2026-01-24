package com.rdc.admin.controller;

import com.rdc.admin.dto.CategoryCreateRequest;
import com.rdc.admin.dto.CategoryResponse;
import com.rdc.admin.dto.CategoryUpdateRequest;
import com.rdc.admin.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * ✅ PUBLIC: Get all categories for Storefront
     * Matches SecurityConfig: .requestMatchers(HttpMethod.GET, "/api/categories/public").permitAll()
     */
    @GetMapping("/api/categories/public")
    public ResponseEntity<List<CategoryResponse>> getAllCategoriesPublic() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * 🔒 ADMIN: Create Category
     * Matches SecurityConfig: .requestMatchers("/api/admin/**").hasRole("ADMIN")
     */
    @PostMapping("/api/admin/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse newCategory = categoryService.createCategory(request);
        return new ResponseEntity<>(newCategory, HttpStatus.CREATED);
    }

    @GetMapping("/api/admin/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategoriesAdmin() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/api/admin/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @PutMapping("/api/admin/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        CategoryResponse updatedCategory = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/api/admin/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}