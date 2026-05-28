package com.adpilot.backend.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String businessName;
    private String industry;
    private String websiteUrl;
    private String facebookUrl;
    private String instagramUrl;

    private String monthlyBudget;
    private String currentCpl;
    private String pixelInstalled;
    private String currentlyRunningAds;
    private String adsExperience;
    private String mainGoal;

    private String averagePrice;
    private String monthlyRevenue;
    private String revenueFromAdsPct;
    private String conversionRate;
    private String monthlyCustomersFromAds;

    private Integer overallScore;
    private String estimatedMonthlyWaste;

    @Column(length = 5000)
    private String fullResultJson;

    private String email;

    @CreationTimestamp
    private LocalDateTime createdAt;
}