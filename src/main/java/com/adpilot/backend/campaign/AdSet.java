package com.adpilot.backend.campaign;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ad_sets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    private String platformAdSetId;
    private String name;
    private String status;

    // Budget
    private Double dailyBudget;
    private Double lifetimeBudget;
    private String currency;

    // Performance
    private Double spend;
    private Long impressions;
    private Long clicks;
    private Double ctr;
    private Double cpc;
    private Double cpm;
    private Double reach;
    private Double frequency;
    private Double costPerResult;
    private Long results;

    // Targeting
    private String optimizationGoal;
    private String billingEvent;
    private String bidStrategy;
    private Double bidAmount;
    private Integer minAge;
    private Integer maxAge;
    private String genders;

    @Column(length = 2000)
    private String targeting;

    // Schedule
    private String startTime;
    private String endTime;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastSyncedAt;
}