package com.adpilot.backend.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdSetRepository extends JpaRepository<AdSet, String> {
    List<AdSet> findByCampaignId(String campaignId);
    Optional<AdSet> findByPlatformAdSetId(String platformAdSetId);
    boolean existsByPlatformAdSetId(String platformAdSetId);
}