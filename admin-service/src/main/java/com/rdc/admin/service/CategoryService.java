package com.rdc.admin.service;

import com.rdc.admin.dto.CategoryCreateRequest;
import com.rdc.admin.dto.CategoryResponse;
import com.rdc.admin.dto.CategoryUpdateRequest;
import com.rdc.admin.entity.Category;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.CategoryRepository;
import com.rdc.admin.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // --- Mapper Utility ---
    private CategoryResponse toResponse(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private Category getCategoryEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    // --- CRUD Operations ---
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        // 1. Check for unique name
        if (categoryRepository.existsByName(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists: " + request.getName());
        }

        // 2. Generate Unique Slug (reusing the existing SlugGenerator utility)
        String baseSlug = SlugGenerator.generateSlug(request.getName());
        String finalSlug = baseSlug;
        int counter = 1;
        while (categoryRepository.existsBySlug(finalSlug)) {
            finalSlug = baseSlug + "-" + counter++;
        }

        Category newCategory = Category.builder()
                .name(request.getName())
                .slug(finalSlug)
                .description(request.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(newCategory);
        return toResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        return toResponse(getCategoryEntityById(id));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category existingCategory = getCategoryEntityById(id);

        // Update Name and Slug if name is provided and changed
        if (request.getName() != null && !request.getName().isBlank()) {
            if (!existingCategory.getName().equalsIgnoreCase(request.getName())) {
                // Check for unique name if changing
                if (categoryRepository.existsByName(request.getName())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists: " + request.getName());
                }

                existingCategory.setName(request.getName());

                // Regenerate slug based on the new name
                String newBaseSlug = SlugGenerator.generateSlug(request.getName());
                String newFinalSlug = newBaseSlug;
                int counter = 1;
                while (categoryRepository.existsBySlug(newFinalSlug)) {
                    newFinalSlug = newBaseSlug + "-" + counter++;
                }
                existingCategory.setSlug(newFinalSlug);
            }
        }

        if (request.getDescription() != null) {
            existingCategory.setDescription(request.getDescription());
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        return toResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category existingCategory = getCategoryEntityById(id);
        // NOTE: In a real system, you must check if any Designs are linked to this Category
        // and prevent deletion, or handle cascading (e.g., setting categoryId to null).
        categoryRepository.delete(existingCategory);
    }
}