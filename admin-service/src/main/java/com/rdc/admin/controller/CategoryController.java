package com.rdc.admin.controller;

import com.rdc.admin.dto.CategoryCreateRequest;
import com.rdc.admin.dto.CategoryDto;
import com.rdc.admin.dto.CategoryUpdateRequest;
import com.rdc.admin.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // GET /api/admin/categories - list categories
    @GetMapping
    public List<CategoryDto> listCategories() {
        // Pagination logic would be added here later (e.g., Pageable object)
        return categoryService.findAll();
    }

    // POST /api/admin/categories - create category
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryDto created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/admin/categories/{id} - update category
    @PutMapping("/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @RequestBody CategoryUpdateRequest request) {
        // Note: Validation on update fields should happen in service/dto
        return categoryService.updateCategory(id, request);
    }

    // DELETE /api/admin/categories/{id} - soft-delete (set active=false)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }
}