package com.adpilot.backend.business;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getBusiness(@PathVariable String userId) {
        return businessService.getBusiness(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Business> updateBusiness(
            @PathVariable String userId,
            @RequestBody Map<String, Object> updates) {
        Business business = businessService.updateBusiness(userId, updates);
        return ResponseEntity.ok(business);
    }

    @GetMapping("/{userId}/onboarding-status")
    public ResponseEntity<Map<String, Boolean>> getOnboardingStatus(@PathVariable String userId) {
        boolean complete = businessService.isOnboardingComplete(userId);
        return ResponseEntity.ok(Map.of("complete", complete));
    }
}