package com.adpilot.backend.business;

import com.adpilot.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "businesses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Step 1 — URLs
    private String websiteUrl;
    private String facebookPageUrl;
    private String instagramUrl;
    private String tiktokUrl;

    // Step 2 — Business details (AI pre-filled)
    private String businessName;
    private String industry;
    private String city;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String services;

    @Column(length = 500)
    private String uniqueSellingPoint;

    private String priceRange;
    private String brandTone;

    // Step 3 — Customer details
    @Column(length = 500)
    private String targetAudience;

    private Integer minAge;
    private Integer maxAge;
    private String gender;
    private String customerSource;
    private Double averageCustomerValue;
    private String buyingCycle;

    // Step 4 — Goals
    private String mainGoal;
    private Double monthlyBudget;
    private Double targetCpl;
    private String phoneNumber;
    private String biggestChallenge;

    // Step 5 — Competition
    @Column(length = 1000)
    private String competitors;

    @Column(length = 500)
    private String competitorAdvantage;

    @Column(length = 500)
    private String ourAdvantage;

    // Meta
    private Integer healthScore;

    @Builder.Default
    private Boolean onboardingComplete = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}