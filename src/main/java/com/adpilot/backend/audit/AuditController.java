package com.adpilot.backend.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditResultRepository auditResultRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/save")
    public ResponseEntity<AuditResult> saveAudit(@RequestBody Map<String, Object> payload) {
        try {
            AuditResult result = AuditResult.builder()
                    .businessName((String) payload.getOrDefault("business_name", ""))
                    .industry((String) payload.getOrDefault("industry", ""))
                    .websiteUrl((String) payload.getOrDefault("website_url", ""))
                    .facebookUrl((String) payload.getOrDefault("facebook_page_url", ""))
                    .instagramUrl((String) payload.getOrDefault("instagram_url", ""))
                    .monthlyBudget((String) payload.getOrDefault("monthly_budget", ""))
                    .currentCpl((String) payload.getOrDefault("current_cpl", ""))
                    .pixelInstalled((String) payload.getOrDefault("pixel_installed", ""))
                    .currentlyRunningAds((String) payload.getOrDefault("currently_running_ads", ""))
                    .adsExperience((String) payload.getOrDefault("ads_experience", ""))
                    .mainGoal((String) payload.getOrDefault("main_goal", ""))
                    .averagePrice((String) payload.getOrDefault("average_price", ""))
                    .monthlyRevenue((String) payload.getOrDefault("monthly_revenue", ""))
                    .revenueFromAdsPct((String) payload.getOrDefault("revenue_from_ads_pct", ""))
                    .conversionRate((String) payload.getOrDefault("conversion_rate", ""))
                    .monthlyCustomersFromAds((String) payload.getOrDefault("monthly_customers_from_ads", ""))
                    .overallScore((Integer) payload.getOrDefault("overall_score", 0))
                    .estimatedMonthlyWaste((String) payload.getOrDefault("estimated_monthly_waste", ""))
                    .email((String) payload.getOrDefault("email", ""))
                    .fullResultJson(objectMapper.writeValueAsString(payload.getOrDefault("full_result", "")))
                    .build();

            AuditResult saved = auditResultRepository.save(result);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/results")
    public ResponseEntity<List<AuditResult>> getAllResults() {
        return ResponseEntity.ok(auditResultRepository.findAllByOrderByCreatedAtDesc());
    }
}