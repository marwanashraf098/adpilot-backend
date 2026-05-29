package com.adpilot.backend.campaign;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CampaignController {

    private final CampaignService campaignService;
    private final AdSetRepository adSetRepository;
    private final AdRepository adRepository;

    @GetMapping("/sync")
    public ResponseEntity<List<Campaign>> syncCampaigns(@RequestParam String userId) {
        return ResponseEntity.ok(campaignService.syncCampaigns(userId));
    }

    @GetMapping
    public ResponseEntity<List<Campaign>> getCampaigns(@RequestParam String userId) {
        return ResponseEntity.ok(campaignService.getUserCampaigns(userId));
    }

    @GetMapping("/{campaignId}/adsets")
    public ResponseEntity<List<AdSet>> getAdSets(@PathVariable String campaignId) {
        return ResponseEntity.ok(adSetRepository.findByCampaignId(campaignId));
    }

    @GetMapping("/adsets/{adSetId}/ads")
    public ResponseEntity<List<Ad>> getAds(@PathVariable String adSetId) {
        return ResponseEntity.ok(adRepository.findByAdSetId(adSetId));
    }
}