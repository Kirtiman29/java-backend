package com.rdc.admin.repository;

import com.rdc.admin.entity.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    List<BlogPost> findAllByOrderByUpdatedAtDesc();

    List<BlogPost> findByPublishedTrueAndPublishedAtLessThanEqualOrderByPublishedAtDesc(LocalDateTime now);

    List<BlogPost> findByPublishedTrueAndFeaturedTrueAndPublishedAtLessThanEqualOrderByPublishedAtDesc(LocalDateTime now);

    Optional<BlogPost> findBySlug(String slug);

    Optional<BlogPost> findBySlugAndPublishedTrueAndPublishedAtLessThanEqual(String slug, LocalDateTime now);

    boolean existsBySlug(String slug);
}
