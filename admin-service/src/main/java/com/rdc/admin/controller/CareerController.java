package com.rdc.admin.controller;

import com.rdc.admin.dto.ApplicationRequest;
import com.rdc.admin.dto.JobResponse;
import com.rdc.admin.dto.JobCreateRequest;
import com.rdc.admin.entity.JobStatus;
import com.rdc.admin.service.CareerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;

    // --- PUBLIC ENDPOINTS ---
    @GetMapping("/api/public/careers/jobs")
    public ResponseEntity<List<JobResponse>> getOpenJobs() {
        return ResponseEntity.ok(careerService.getOpenJobs());
    }

    @PostMapping("/api/public/careers/apply")
    public ResponseEntity<Void> apply(@Valid @RequestBody ApplicationRequest request) {
        careerService.submitApplication(request);
        return ResponseEntity.ok().build();
    }

    // --- ADMIN ENDPOINTS ---
    @PostMapping("/api/admin/careers/jobs")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobCreateRequest request) {
        return new ResponseEntity<>(careerService.createJob(request), HttpStatus.CREATED);
    }

    @GetMapping("/api/admin/careers/jobs")
    public ResponseEntity<List<JobResponse>> getAllJobsForAdmin() {
        return ResponseEntity.ok(careerService.getAllJobs());
    }

    @PatchMapping("/api/admin/careers/jobs/{id}/status")
    public ResponseEntity<Void> toggleJobStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        JobStatus status = JobStatus.valueOf(body.get("status").toUpperCase());
        careerService.updateJobStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/admin/careers/jobs/{id}/applications")
    public ResponseEntity<List<Map<String, Object>>> getJobApplications(@PathVariable Long id) {
        return ResponseEntity.ok(careerService.getApplicationsForJob(id));
    }
}