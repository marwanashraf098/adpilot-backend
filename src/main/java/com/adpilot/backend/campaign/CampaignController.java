package com.adpilot.backend.campaign;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

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

    @PostMapping(value = "/create", consumes = {org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Map<String, Object>> createCampaign(
            @RequestParam String userId,
            @RequestParam String name,
            @RequestParam String objective,
            @RequestParam String adSetName,
            @RequestParam String optimizationGoal,
            @RequestParam String dailyBudget,
            @RequestParam String targeting,
            @RequestParam(required = false) String headline,
            @RequestParam(required = false) String body,
            @RequestParam(required = false) String cta,
            @RequestParam(required = false) String linkUrl,
            @RequestParam(required = false) String imageUrls,
            @RequestParam(required = false) String headlines,
            @RequestParam(required = false) String bodies,
            @RequestParam(required = false) List<org.springframework.web.multipart.MultipartFile> images,
            @RequestParam(required = false) List<org.springframework.web.multipart.MultipartFile> videos) {

        Map<String, Object> campaignData = new java.util.HashMap<>();
        campaignData.put("name", name);
        campaignData.put("objective", objective);
        campaignData.put("adSetName", adSetName);
        campaignData.put("optimizationGoal", optimizationGoal);
        campaignData.put("dailyBudget", dailyBudget);
        campaignData.put("targeting", targeting);
        campaignData.put("headline", headline);
        campaignData.put("body", body);
        campaignData.put("cta", cta != null ? cta : "LEARN_MORE");
        campaignData.put("linkUrl", linkUrl != null ? linkUrl : "https://adpilot-frontend-chi.vercel.app");
        campaignData.put("imageUrls", imageUrls);
        campaignData.put("headlines", headlines);
        campaignData.put("bodies", bodies);
        campaignData.put("images", images);
        campaignData.put("videos", videos);

        return ResponseEntity.ok(campaignService.createCampaign(userId, campaignData));
    }

    @PostMapping("/execute-action")
    public ResponseEntity<Map<String, Object>> executeAction(
            @RequestParam String userId,
            @RequestBody Map<String, Object> action) {
        return ResponseEntity.ok(campaignService.executeAction(userId, action));
    }
}