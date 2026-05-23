package com.adpilot.backend.adaccount;

import com.adpilot.backend.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "ad_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String platform; // META, GOOGLE, TIKTOK

    @Column(nullable = false)
    private String platformAccountId;

    @Column(nullable = false, length = 2000)
    private String accessToken;

    private String accountName;

    private String currency;

    @Column(nullable = false)
    private String status; // ACTIVE, EXPIRED, DISCONNECTED

    @Column(nullable = false, updatable = false)
    private LocalDateTime connectedAt;

    @PrePersist
    public void prePersist() {
        this.connectedAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }
}