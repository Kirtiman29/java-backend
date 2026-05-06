package com.rdc.admin.newsletter.repository;

import com.rdc.admin.newsletter.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

    Optional<NewsletterSubscriber> findByEmailIgnoreCase(String email);

    List<NewsletterSubscriber> findAllByOrderBySubscribedAtDesc();

    List<NewsletterSubscriber> findAllByActiveTrueOrderBySubscribedAtAsc();
}
