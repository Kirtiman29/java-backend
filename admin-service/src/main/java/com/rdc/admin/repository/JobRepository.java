package com.rdc.admin.repository;

import com.rdc.admin.entity.Job;
import com.rdc.admin.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);
    Optional<Job> findBySlug(String slug);
    boolean existsBySlug(String slug);
}