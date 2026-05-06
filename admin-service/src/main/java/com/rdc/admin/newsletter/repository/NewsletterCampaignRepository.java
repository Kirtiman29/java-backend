package com.rdc.admin.newsletter.repository;

import com.rdc.admin.newsletter.entity.NewsletterCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsletterCampaignRepository extends JpaRepository<NewsletterCampaign, Long> {

    List<NewsletterCampaign> findAllByOrderByCreatedAtDesc();
}
