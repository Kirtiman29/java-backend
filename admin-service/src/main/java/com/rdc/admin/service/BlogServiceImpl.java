package com.rdc.admin.service;

import com.rdc.admin.dto.BlogCreateRequest;
import com.rdc.admin.dto.BlogResponse;
import com.rdc.admin.dto.BlogUpdateRequest;
import com.rdc.admin.entity.BlogPost;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private static final int WORDS_PER_MINUTE = 200;
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private final BlogPostRepository blogPostRepository;
    private final AssetClientService assetClientService;

    @Value("${service.asset.url}")
    private String assetServiceBaseUrl;

    @Override
    @Transactional
    public BlogResponse create(BlogCreateRequest request) {
        BlogPost post = new BlogPost();

        post.setTitle(cleanRequired(request.getTitle(), "Title is required"));
        post.setSlug(resolveUniqueSlug(request.getSlug(), request.getTitle(), null));
        post.setExcerpt(clean(request.getExcerpt()));
        post.setContent(clean(request.getContent()));
        post.setCoverAssetUuid(clean(request.getCoverAssetUuid()));
        post.setCoverImageUrl(resolveCoverImageUrl(post.getCoverAssetUuid(), request.getCoverImageUrl()));
        post.setCategory(clean(request.getCategory()));
        post.setAuthorName(cleanOrDefault(request.getAuthorName(), "RDC Editorial"));
        post.setReadingTimeMinutes(resolveReadingTime(request.getReadingTimeMinutes(), post.getContent()));
        post.setPublished(Boolean.TRUE.equals(request.getPublished()));
        post.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        post.setPublishedAt(resolvePublishedAt(request.getPublishedAt(), post.getPublished()));
        post.setSeoTitle(clean(request.getSeoTitle()));
        post.setSeoDescription(clean(request.getSeoDescription()));

        return mapToResponse(blogPostRepository.save(post));
    }

    @Override
    @Transactional
    public BlogResponse update(Long id, BlogUpdateRequest request) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post", id));

        if (request.getTitle() != null) {
            post.setTitle(cleanRequired(request.getTitle(), "Title is required"));
        }
        if (request.getSlug() != null) {
            post.setSlug(resolveUniqueSlug(request.getSlug(), post.getTitle(), id));
        }
        if (request.getExcerpt() != null) post.setExcerpt(clean(request.getExcerpt()));
        if (request.getContent() != null) {
            post.setContent(clean(request.getContent()));
            if (request.getReadingTimeMinutes() == null) {
                post.setReadingTimeMinutes(resolveReadingTime(null, post.getContent()));
            }
        }
        if (request.getCoverAssetUuid() != null) {
            post.setCoverAssetUuid(clean(request.getCoverAssetUuid()));
            post.setCoverImageUrl(resolveCoverImageUrl(post.getCoverAssetUuid(), request.getCoverImageUrl()));
        } else if (request.getCoverImageUrl() != null) {
            post.setCoverImageUrl(clean(request.getCoverImageUrl()));
        }
        if (request.getCategory() != null) post.setCategory(clean(request.getCategory()));
        if (request.getAuthorName() != null) post.setAuthorName(clean(request.getAuthorName()));
        if (request.getReadingTimeMinutes() != null) {
            post.setReadingTimeMinutes(resolveReadingTime(request.getReadingTimeMinutes(), post.getContent()));
        }
        if (request.getPublished() != null) {
            post.setPublished(request.getPublished());
            if (request.getPublished() && post.getPublishedAt() == null) {
                post.setPublishedAt(LocalDateTime.now());
            }
        }
        if (request.getFeatured() != null) post.setFeatured(request.getFeatured());
        if (request.getPublishedAt() != null) post.setPublishedAt(request.getPublishedAt());
        if (request.getSeoTitle() != null) post.setSeoTitle(clean(request.getSeoTitle()));
        if (request.getSeoDescription() != null) post.setSeoDescription(clean(request.getSeoDescription()));

        return mapToResponse(blogPostRepository.save(post));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogResponse> getAllAdmin() {
        return blogPostRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogResponse> getPublished() {
        return blogPostRepository.findByPublishedTrueAndPublishedAtLessThanEqualOrderByPublishedAtDesc(LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogResponse> getFeatured() {
        return blogPostRepository.findByPublishedTrueAndFeaturedTrueAndPublishedAtLessThanEqualOrderByPublishedAtDesc(LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BlogResponse getAdminById(Long id) {
        return blogPostRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post", id));
    }

    @Override
    @Transactional(readOnly = true)
    public BlogResponse getPublishedBySlug(String slug) {
        return blogPostRepository.findBySlugAndPublishedTrueAndPublishedAtLessThanEqual(slug, LocalDateTime.now())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found: " + slug));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post", id));
        blogPostRepository.delete(post);
    }

    private BlogResponse mapToResponse(BlogPost post) {
        LocalDateTime publishedAt = post.getPublishedAt();

        return BlogResponse.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .coverAssetUuid(post.getCoverAssetUuid())
                .coverImageUrl(post.getCoverImageUrl())
                .category(post.getCategory())
                .authorName(post.getAuthorName())
                .readingTimeMinutes(post.getReadingTimeMinutes())
                .publishedAt(publishedAt)
                .publishedDate(publishedAt != null ? publishedAt.toLocalDate() : null)
                .publishedTime(publishedAt != null ? publishedAt.toLocalTime() : null)
                .published(post.getPublished())
                .featured(post.getFeatured())
                .seoTitle(post.getSeoTitle())
                .seoDescription(post.getSeoDescription())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private String resolveCoverImageUrl(String coverAssetUuid, String coverImageUrl) {
        if (coverAssetUuid != null && !coverAssetUuid.isBlank()) {
            assetClientService.validateAsset(coverAssetUuid);
            return assetServiceBaseUrl + "/api/assets/download/" + coverAssetUuid;
        }
        return clean(coverImageUrl);
    }

    private LocalDateTime resolvePublishedAt(LocalDateTime requested, Boolean published) {
        if (requested != null) {
            return requested;
        }
        return Boolean.TRUE.equals(published) ? LocalDateTime.now() : null;
    }

    private Integer resolveReadingTime(Integer requested, String content) {
        if (requested != null && requested > 0) {
            return requested;
        }
        if (content == null || content.isBlank()) {
            return 1;
        }

        int words = content.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / (double) WORDS_PER_MINUTE));
    }

    private String resolveUniqueSlug(String requestedSlug, String title, Long currentId) {
        String baseSlug = slugify(cleanOrDefault(requestedSlug, title));
        String candidate = baseSlug;
        int counter = 2;

        while (blogPostRepository.findBySlug(candidate)
                .filter(post -> currentId == null || !post.getId().equals(currentId))
                .isPresent()) {
            candidate = baseSlug + "-" + counter;
            counter++;
        }

        return candidate;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-");
        slug = trimDash(slug);
        return slug.isBlank() ? "blog-post" : slug;
    }

    private String trimDash(String value) {
        String result = value;
        while (result.startsWith("-")) result = result.substring(1);
        while (result.endsWith("-")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private String cleanRequired(String value, String message) {
        String cleaned = clean(value);
        if (cleaned == null || cleaned.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return cleaned;
    }

    private String cleanOrDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned == null || cleaned.isBlank() ? cleanRequired(fallback, "Slug source is required") : cleaned;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
