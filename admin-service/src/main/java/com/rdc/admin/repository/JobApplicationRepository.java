package com.rdc.admin.repository;

import com.rdc.admin.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByJobIdOrderByAppliedAtDesc(Long jobId);
}