package com.rdc.admin.service;

import com.rdc.admin.dto.CategoryCreateRequest;
import com.rdc.admin.dto.CategoryResponse;
import com.rdc.admin.dto.CategoryUpdateRequest;
import com.rdc.admin.entity.Category;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.CategoryRepository;
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
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
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
                .description(request.getDescription())
                .imageUrl(resolvedImageUrl)
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

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!existingCategory.getName().equalsIgnoreCase(request.getName())) {
                if (categoryRepository.existsByName(request.getName())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists: " + request.getName());
                }
                existingCategory.setName(request.getName());
            }
        }


        if (request.getImageUuid() != null) {
            assetClientService.validateAsset(request.getImageUuid());


            existingCategory.setImageUrl(assetServiceBaseUrl + "/api/assets/" + request.getImageUuid() + "/download");
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