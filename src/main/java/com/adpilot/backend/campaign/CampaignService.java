package com.adpilot.backend.campaign;

import com.adpilot.backend.adaccount.AdAccount;
import com.adpilot.backend.adaccount.AdAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AdAccountRepository adAccountRepository;
    private final WebClient.Builder webClientBuilder;

    // Pull campaigns from Meta API and save to database
    public List<Campaign> syncCampaigns(String userId) {
        List<AdAccount> accounts = adAccountRepository.findByUserId(userId);

        for (AdAccount account : accounts) {
            if (!account.getPlatform().equals("META")) continue;

            String accessToken = account.getAccessToken();
            String accountId = account.getPlatformAccountId();

            // Fetch campaigns from Meta
            Map response = webClientBuilder.build()
                    .get()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/campaigns" +
                            "?fields=id,name,objective,status,daily_budget,lifetime_budget" +
                            "&access_token=" + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) continue;

            List<Map> campaigns = (List<Map>) response.get("data");
            if (campaigns == null) continue;

            for (Map c : campaigns) {
                String platformId = (String) c.get("id");

                // Update if exists, create if not
                Campaign campaign = campaignRepository
                        .findByPlatformCampaignId(platformId)
                        .orElse(Campaign.builder()
                                .adAccount(account)
                                .platformCampaignId(platformId)
                                .currency(account.getCurrency())
                                .build());

                campaign.setName((String) c.get("name"));
                campaign.setObjective((String) c.get("objective"));
                campaign.setStatus((String) c.get("status"));
                campaign.setLastSyncedAt(LocalDateTime.now());

                // Parse budgets
                Object dailyBudget = c.get("daily_budget");
                if (dailyBudget != null) {
                    campaign.setDailyBudget(Double.parseDouble(dailyBudget.toString()) / 100);
                }

                Object lifetimeBudget = c.get("lifetime_budget");
                if (lifetimeBudget != null) {
                    campaign.setLifetimeBudget(Double.parseDouble(lifetimeBudget.toString()) / 100);
                }

                campaignRepository.save(campaign);
            }

            // Fetch insights (spend, clicks, impressions)
            syncInsights(account, accessToken, accountId);
        }

        return campaignRepository.findByAdAccountUserId(userId);
    }

    private void syncInsights(AdAccount account, String accessToken, String accountId) {
        try {
            Map response = webClientBuilder.build()
                    .get()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/campaigns" +
                            "?fields=id,name,objective,status,daily_budget,lifetime_budget" +
                            "&effective_status=[\"ACTIVE\",\"PAUSED\",\"DELETED\",\"ARCHIVED\",\"IN_PROCESS\",\"WITH_ISSUES\"]" +
                            "&access_token=" + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return;
            List<Map> insights = (List<Map>) response.get("data");
            if (insights == null) return;

            for (Map insight : insights) {
                String platformId = (String) insight.get("campaign_id");
                campaignRepository.findByPlatformCampaignId(platformId).ifPresent(campaign -> {
                    campaign.setSpend(parseDouble(insight.get("spend")));
                    campaign.setImpressions(parseLong(insight.get("impressions")));
                    campaign.setClicks(parseLong(insight.get("clicks")));
                    campaign.setCtr(parseDouble(insight.get("ctr")));
                    campaign.setCpc(parseDouble(insight.get("cpc")));
                    campaign.setCpm(parseDouble(insight.get("cpm")));
                    campaign.setLastSyncedAt(LocalDateTime.now());
                    campaignRepository.save(campaign);
                });
            }
        } catch (Exception e) {
            System.out.println("Insights sync failed: " + e.getMessage());
        }
    }

    public List<Campaign> getUserCampaigns(String userId) {
        return campaignRepository.findByAdAccountUserId(userId);
    }

    private Double parseDouble(Object val) {
        if (val == null) return null;
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private Long parseLong(Object val) {
        if (val == null) return null;
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}