package com.rdc.admin.service;

import com.rdc.admin.dto.ApplicationRequest;
import com.rdc.admin.dto.JobCreateRequest;
import com.rdc.admin.dto.JobResponse;
import com.rdc.admin.entity.Job;
import com.rdc.admin.entity.JobApplication;
import com.rdc.admin.entity.JobStatus;
import com.rdc.admin.repository.JobApplicationRepository;
import com.rdc.admin.repository.JobRepository;
import com.rdc.admin.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final SmtpEmailService emailService;


    @Value("${service.asset.url}")
    private String assetServiceBaseUrl;

    @Value("${rdc.hr.email}")
    private String hrRecipientEmail;

    @Transactional
    public JobResponse createJob(JobCreateRequest request) {
        Job job = Job.builder()
                .title(request.getTitle())
                .slug(generateUniqueSlug(request.getTitle()))
                .description(request.getDescription())
                .location(request.getLocation())
                .experienceLevel(request.getExperienceLevel())
                .jobType(request.getJobType())
                .status(JobStatus.OPEN)
                .build();

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getOpenJobs() {
        return jobRepository.findByStatus(JobStatus.OPEN).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void submitApplication(ApplicationRequest req) {
        Job job = jobRepository.findById(req.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found."));

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new IllegalStateException("Hiring for this position is CLOSED.");
        }

        JobApplication application = JobApplication.builder()
                .job(job)
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .resumeAssetUuid(req.getResumeAssetUuid())
                .build();

        applicationRepository.save(application);

        Map<String, Object> candidateVars = new HashMap<>();
        candidateVars.put("jobTitle", job.getTitle());
        emailService.sendHtmlEmail(req.getEmail(), "Application Received – RDC Careers", "candidate-confirmation", candidateVars);

        String resumeLink = assetServiceBaseUrl + "/api/assets/download/" + req.getResumeAssetUuid();
        Map<String, Object> hrVars = new HashMap<>();
        hrVars.put("jobTitle", job.getTitle());
        hrVars.put("candidateName", req.getFullName());
        hrVars.put("candidateEmail", req.getEmail());
        hrVars.put("resumeLink", resumeLink);

        emailService.sendHtmlEmail(hrRecipientEmail, "RDC ALERT: New Job Applicant", "hr-job-alert", hrVars);
    }

    @Transactional
    public void updateJobStatus(Long jobId, JobStatus status) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(status);
        jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApplicationsForJob(Long jobId) {
        return applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId).stream()
                .map(app -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", app.getId());
                    map.put("name", app.getFullName());
                    map.put("email", app.getEmail());
                    map.put("phone", (app.getPhone() != null ? app.getPhone() : "N/A"));
                    // ✅ FIXED: Uses dynamic base URL instead of hardcoded localhost
                    map.put("resumeUrl", assetServiceBaseUrl + "/api/assets/download/" + app.getResumeAssetUuid());
                    map.put("appliedAt", app.getAppliedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = SlugGenerator.generateSlug(title);
        String slug = baseSlug;
        int count = 1;
        while (jobRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + count++;
        }
        return slug;
    }

    private JobResponse mapToResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .slug(job.getSlug())
                .description(job.getDescription())
                .location(job.getLocation())
                .experienceLevel(job.getExperienceLevel())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .build();
    }
}