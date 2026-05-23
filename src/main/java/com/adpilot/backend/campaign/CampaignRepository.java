package com.adpilot.backend.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, String> {
    List<Campaign> findByAdAccountId(String adAccountId);
    List<Campaign> findByAdAccountUserId(String userId);
    Optional<Campaign> findByPlatformCampaignId(String platformCampaignId);
}