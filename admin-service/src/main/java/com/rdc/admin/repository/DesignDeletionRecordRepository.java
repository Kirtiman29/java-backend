package com.rdc.admin.repository;

import com.rdc.admin.entity.DesignDeletionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignDeletionRecordRepository
        extends JpaRepository<DesignDeletionRecord, Long> {

    List<DesignDeletionRecord> findAllByOrderByDeletedAtDesc();
}