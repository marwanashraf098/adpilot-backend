package com.adpilot.backend.campaign;

import com.adpilot.backend.adaccount.AdAccount;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "ad_account_id", nullable = false)
    private AdAccount adAccount;

    @Column(nullable = false)
    private String platformCampaignId;

    @Column(nullable = false)
    private String name;

    private String objective;
    private String status;
    private Double dailyBudget;
    private Double lifetimeBudget;
    private String currency;

    // Performance metrics
    private Double spend;
    private Long impressions;
    private Long clicks;
    private Double ctr;
    private Double cpc;
    private Double cpm;
    private Integer conversions;
    private Double costPerResult;
    private Double roas;

    private LocalDateTime lastSyncedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}