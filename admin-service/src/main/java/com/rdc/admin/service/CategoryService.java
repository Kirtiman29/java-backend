package com.rdc.admin.service;

import com.rdc.admin.dto.CategoryCreateRequest;
import com.rdc.admin.dto.CategoryDto;
import com.rdc.admin.dto.CategoryUpdateRequest;
import com.rdc.admin.entity.Category;
import com.rdc.admin.repository.CategoryRepository;
import com.rdc.admin.util.SlugGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Maps the Category Entity to the Category DTO for responses.
     */
    private CategoryDto mapToDto(Category entity) {
        return CategoryDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .active(entity.isActive())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Creates a new Category entity and saves it to the database.
     */
    public CategoryDto createCategory(CategoryCreateRequest request) {
        // 1. Validation: Check for unique name
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists.");
        }

        // 2. Mapping and setting default values
        Category category = new Category();
        category.setName(request.getName());

        // Use SlugGenerator utility
        category.setSlug(request.getSlug() != null ? request.getSlug() : SlugGenerator.generateSlug(request.getName()));

        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setActive(request.getActive());
        category.setSortOrder(request.getSortOrder());
        category.setCreatedAt(LocalDateTime.now());

        // 3. Save and return DTO
        Category saved = categoryRepository.save(category);
        return mapToDto(saved);
    }

    /**
     * Retrieves all categories.
     */
    public List<CategoryDto> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing Category.
     */
    public CategoryDto updateCategory(Long id, CategoryUpdateRequest request) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found."));

        // Only update fields if they are provided in the request
        if (request.getName() != null) {
            existing.setName(request.getName());
            // Re-generate slug if name changes and a new slug isn't provided
            if (request.getSlug() == null) {
                existing.setSlug(SlugGenerator.generateSlug(request.getName()));
            }
        }
        if (request.getSlug() != null) {
            existing.setSlug(request.getSlug());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            existing.setImageUrl(request.getImageUrl());
        }
        if (request.getActive() != null) {
            existing.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }

        Category updated = categoryRepository.save(existing);
        return mapToDto(updated);
    }

    /**
     * Soft Delete: Sets the category to inactive.
     */
    public void deleteCategory(Long id) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found."));

        existing.setActive(false); // Soft Delete
        categoryRepository.save(existing);
    }
}