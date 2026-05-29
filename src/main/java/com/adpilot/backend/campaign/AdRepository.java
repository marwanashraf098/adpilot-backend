package com.adpilot.backend.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdRepository extends JpaRepository<Ad, String> {
    List<Ad> findByAdSetId(String adSetId);
    Optional<Ad> findByPlatformAdId(String platformAdId);
    boolean existsByPlatformAdId(String platformAdId);
}
