package com.rdc.admin.repository;

import com.rdc.admin.entity.AdminTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AdminTransactionRepository extends JpaRepository<AdminTransaction, Long> {

    // Custom query for filtering transactions by date and type
    List<AdminTransaction> findByTimestampBetweenAndType(LocalDateTime from, LocalDateTime to, String type);

    // Custom query for filtering transactions by date only
    List<AdminTransaction> findByTimestampBetween(LocalDateTime from, LocalDateTime to);

    // Aggregated Sales Report Query
    // Note: Use native query or Spring Data JPA projections/named queries for complex reports
    @Query(value = "SELECT new Map(t.designId as designId, SUM(t.amountCents) as totalCents, COUNT(t.id) as count) " +
            "FROM AdminTransaction t " +
            "WHERE t.timestamp BETWEEN :from AND :to AND t.type = 'SALE' " +
            "GROUP BY t.designId " +
            "ORDER BY totalCents DESC")
    List<Map<String, Object>> getAggregatedSalesByDesign(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}