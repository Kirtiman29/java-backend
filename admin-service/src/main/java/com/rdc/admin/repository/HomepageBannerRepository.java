package com.rdc.admin.repository;

import com.rdc.admin.entity.BannerTheme;
import com.rdc.admin.entity.HomepageBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HomepageBannerRepository extends JpaRepository<HomepageBanner, Long> {

    @Query("SELECT b FROM HomepageBanner b " +
            "WHERE b.active = true " +
            "AND :today BETWEEN b.startDate AND b.endDate " +
            "ORDER BY b.priority DESC")
    List<HomepageBanner> findActiveBanners(@Param("today") LocalDate today);

    Optional<HomepageBanner> findByTheme(BannerTheme theme);
}