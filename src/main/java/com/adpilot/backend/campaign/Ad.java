package com.adpilot.backend.campaign;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "ad_set_id", nullable = false)
    private AdSet adSet;

    private String platformAdId;
    private String name;
    private String status;

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

    // Creative
    private String creativeId;
    private String creativeFormat;
    private String headline;
    private String body;
    private String description;
    private String ctaType;
    private String linkUrl;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 1000)
    private String previewUrl;

    private String videoId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastSyncedAt;
}