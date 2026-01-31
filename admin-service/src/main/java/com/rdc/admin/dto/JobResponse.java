// JobResponse.java
package com.rdc.admin.dto;

import com.rdc.admin.entity.JobStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class JobResponse {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private String location;
    private String experienceLevel;
    private String jobType;
    private JobStatus status;
    private LocalDateTime createdAt;
}

