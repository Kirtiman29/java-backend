package com.rdc.admin.repository;

import com.rdc.admin.entity.DesignDeletionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignDeletionRecordRepository
        extends JpaRepository<DesignDeletionRecord, Long> {

    /**
     * ✅ Custom Query: Fetches all audit records sorted by deletion time.
     * This ensures the React frontend shows the latest purges (sales) at the top.
     */
    List<DesignDeletionRecord> findAllByOrderByDeletedAtDesc();
}