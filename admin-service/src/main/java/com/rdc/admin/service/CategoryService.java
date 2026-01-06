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
    private final AssetClientService assetClientService; // Injected to validate imageUuid [cite: 228, 231]

    // --- Mapper Utility ---
    private CategoryResponse toResponse(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl()) // Map the URL to the response
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
        if (categoryRepository.existsByName(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists: " + request.getName());
        }

        // 1. Handle Image Logic
        String resolvedImageUrl = null;
        if (request.getImageUuid() != null && !request.getImageUuid().isBlank()) {
            assetClientService.validateAsset(request.getImageUuid()); // Validate via Port 8090
            resolvedImageUrl = "http://localhost:8090/api/assets/" + request.getImageUuid() + "/download";
        }

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
                .imageUrl(resolvedImageUrl) // Save the URL in the DB
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

        // Update Name/Slug
        if (request.getName() != null && !request.getName().isBlank()) {
            if (!existingCategory.getName().equalsIgnoreCase(request.getName())) {
                if (categoryRepository.existsByName(request.getName())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists: " + request.getName());
                }
                existingCategory.setName(request.getName());
                String newBaseSlug = SlugGenerator.generateSlug(request.getName());
                String newFinalSlug = newBaseSlug;
                int counter = 1;
                while (categoryRepository.existsBySlug(newFinalSlug)) {
                    newFinalSlug = newBaseSlug + "-" + counter++;
                }
                existingCategory.setSlug(newFinalSlug);
            }
        }

        // 2. Handle Image Update
        if (request.getImageUuid() != null) {
            assetClientService.validateAsset(request.getImageUuid());
            existingCategory.setImageUrl("http://localhost:8090/api/assets/" + request.getImageUuid() + "/download");
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
        categoryRepository.delete(existingCategory);
    }
}