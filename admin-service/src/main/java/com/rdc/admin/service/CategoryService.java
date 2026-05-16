package com.rdc.admin.service;

import com.rdc.admin.dto.CategoryCreateRequest;
import com.rdc.admin.dto.CategoryResponse;
import com.rdc.admin.dto.CategoryUpdateRequest;
import com.rdc.admin.entity.Category;
import com.rdc.admin.entity.CategoryScope;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.CategoryRepository;
import com.rdc.admin.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final AssetClientService assetClientService;


    @Value("${service.asset.url}")
    private String assetServiceBaseUrl;


    private CategoryResponse toResponse(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .scope(category.getScope())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private Category getCategoryEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }


    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists: " + request.getName());
        }


        String resolvedImageUrl = null;
        if (request.getImageUuid() != null && !request.getImageUuid().isBlank()) {
            assetClientService.validateAsset(request.getImageUuid());


            resolvedImageUrl = assetServiceBaseUrl + "/api/assets/" + request.getImageUuid() + "/download";
        }


        Category newCategory = Category.builder()
                .name(request.getName())
                .slug(resolveSlug(request.getSlug(), request.getName(), null))
                .description(request.getDescription())
                .imageUrl(resolvedImageUrl)
                .scope(request.getScope() != null ? request.getScope() : CategoryScope.BOTH)
                .build();

        Category savedCategory = categoryRepository.save(newCategory);
        return toResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories(CategoryScope scope) {
        return categoryRepository.findAll().stream()
                .filter(category -> scope == null || supportsScope(category.getScope(), scope))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        return toResponse(getCategoryEntityById(id));
    }

    public CategoryResponse getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category existingCategory = getCategoryEntityById(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!existingCategory.getName().equalsIgnoreCase(request.getName())) {
                if (categoryRepository.existsByName(request.getName())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists: " + request.getName());
                }
                existingCategory.setName(request.getName());
            }
        }

        if (hasText(request.getSlug())) {
            existingCategory.setSlug(resolveSlug(request.getSlug(), existingCategory.getName(), id));
        } else if (existingCategory.getSlug() == null || existingCategory.getSlug().isBlank()) {
            existingCategory.setSlug(resolveSlug(null, existingCategory.getName(), id));
        }


        if (request.getImageUuid() != null) {
            assetClientService.validateAsset(request.getImageUuid());


            existingCategory.setImageUrl(assetServiceBaseUrl + "/api/assets/" + request.getImageUuid() + "/download");
        }

        if (request.getDescription() != null) {
            existingCategory.setDescription(request.getDescription());
        }

        if (request.getScope() != null) {
            existingCategory.setScope(request.getScope());
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        return toResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category existingCategory = getCategoryEntityById(id);
        categoryRepository.delete(existingCategory);
    }

    @Transactional
    public int backfillMissingSlugs() {
        int updated = 0;

        for (Category category : categoryRepository.findAllBySlugIsNull()) {
            category.setSlug(resolveSlug(null, category.getName(), category.getId()));
            categoryRepository.save(category);
            updated++;
        }

        return updated;
    }

    private String resolveSlug(String requestedSlug, String fallback, Long currentId) {
        String sanitizedRequested = sanitizeSlug(requestedSlug);
        if (sanitizedRequested != null) {
            categoryRepository.findBySlug(sanitizedRequested).ifPresent(existing -> {
                if (currentId == null || !existing.getId().equals(currentId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug already exists");
                }
            });
            return sanitizedRequested;
        }

        String baseSlug = SlugUtil.generateSlug(fallback);
        if (baseSlug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug source is required");
        }

        String candidate = baseSlug;
        int counter = 2;
        while (categoryRepository.findBySlug(candidate)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .isPresent()) {
            candidate = SlugUtil.withSuffix(baseSlug, counter);
            counter++;
        }

        return candidate;
    }

    private String sanitizeSlug(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String sanitized = SlugUtil.generateSlug(value);
        if (!SlugUtil.isValidSlug(sanitized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid slug format");
        }
        return sanitized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean supportsScope(CategoryScope categoryScope, CategoryScope requestedScope) {
        CategoryScope effectiveScope = categoryScope != null ? categoryScope : CategoryScope.BOTH;
        return switch (requestedScope) {
            case DESIGN -> effectiveScope.supportsDesign();
            case FABRIC -> effectiveScope.supportsFabric();
            case BOTH -> effectiveScope == CategoryScope.BOTH;
        };
    }
}
