package com.adpilot.backend.campaign;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping("/sync")
    public ResponseEntity<List<Campaign>> syncCampaigns(@RequestParam String userId) {
        return ResponseEntity.ok(campaignService.syncCampaigns(userId));
    }

    @GetMapping
    public ResponseEntity<List<Campaign>> getCampaigns(@RequestParam String userId) {
        return ResponseEntity.ok(campaignService.getUserCampaigns(userId));
    }
}