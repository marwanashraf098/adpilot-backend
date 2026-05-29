package com.adpilot.backend.campaign;

import com.adpilot.backend.adaccount.AdAccount;
import com.adpilot.backend.adaccount.AdAccountRepository;
import com.adpilot.backend.notification.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final AdSetRepository adSetRepository;
    private final AdRepository adRepository;
    private final WebClient.Builder webClientBuilder;
    private final WhatsAppService whatsAppService;
    private final com.adpilot.backend.business.BusinessRepository businessRepository;

    public List<Campaign> syncCampaigns(String userId) {
        List<AdAccount> accounts = adAccountRepository.findByUserId(userId);

        for (AdAccount account : accounts) {
            if (!account.getPlatform().equals("META")) continue;

            String accessToken = account.getAccessToken();
            String accountId = account.getPlatformAccountId();

            try {
                // Fetch campaigns with maximum fields
                Map response = webClientBuilder.build()
                        .get()
                        .uri("https://graph.facebook.com/v19.0/" + accountId + "/campaigns" +
                                "?fields=id,name,objective,status,daily_budget,lifetime_budget,buying_type,bid_strategy,start_time,stop_time,created_time" +
                                "&access_token=" + accessToken)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (response == null) continue;
                List<Map> campaigns = (List<Map>) response.get("data");
                if (campaigns == null) continue;

                for (Map c : campaigns) {
                    String platformId = (String) c.get("id");

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

                    Object dailyBudget = c.get("daily_budget");
                    if (dailyBudget != null) {
                        campaign.setDailyBudget(Double.parseDouble(dailyBudget.toString()) / 100);
                    }
                    Object lifetimeBudget = c.get("lifetime_budget");
                    if (lifetimeBudget != null) {
                        campaign.setLifetimeBudget(Double.parseDouble(lifetimeBudget.toString()) / 100);
                    }

                    Campaign savedCampaign = campaignRepository.save(campaign);

                    // Sync ad sets for this campaign
                    syncAdSets(savedCampaign, accessToken);
                }

                // Sync campaign insights
                syncInsights(account, accessToken, accountId);
                // Sync ad set insights
                syncAdSetInsights(account, accessToken, accountId);
                // Sync ad insights
                syncAdInsights(account, accessToken, accountId);

            } catch (Exception e) {
                System.out.println("Sync failed for account: " + accountId + " - " + e.getMessage());
            }
        }

        return campaignRepository.findByAdAccountUserId(userId);
    }

    private void syncAdSets(Campaign campaign, String accessToken) {
        try {
            Map response = webClientBuilder.build()
                    .get()
                    .uri("https://graph.facebook.com/v19.0/" + campaign.getPlatformCampaignId() + "/adsets" +
                            "?fields=id,name,status,daily_budget,lifetime_budget,optimization_goal,billing_event,bid_strategy,bid_amount,targeting,start_time,end_time" +
                            "&access_token=" + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return;
            List<Map> adSets = (List<Map>) response.get("data");
            if (adSets == null) return;

            for (Map as : adSets) {
                String platformAdSetId = (String) as.get("id");

                AdSet adSet = adSetRepository
                        .findByPlatformAdSetId(platformAdSetId)
                        .orElse(AdSet.builder()
                                .campaign(campaign)
                                .platformAdSetId(platformAdSetId)
                                .currency(campaign.getCurrency())
                                .build());

                adSet.setName((String) as.get("name"));
                adSet.setStatus((String) as.get("status"));
                adSet.setOptimizationGoal((String) as.get("optimization_goal"));
                adSet.setBillingEvent((String) as.get("billing_event"));
                adSet.setBidStrategy((String) as.get("bid_strategy"));
                adSet.setStartTime((String) as.get("start_time"));
                adSet.setEndTime((String) as.get("end_time"));
                adSet.setLastSyncedAt(LocalDateTime.now());

                Object dailyBudget = as.get("daily_budget");
                if (dailyBudget != null) {
                    adSet.setDailyBudget(Double.parseDouble(dailyBudget.toString()) / 100);
                }
                Object lifetimeBudget = as.get("lifetime_budget");
                if (lifetimeBudget != null) {
                    adSet.setLifetimeBudget(Double.parseDouble(lifetimeBudget.toString()) / 100);
                }
                Object bidAmount = as.get("bid_amount");
                if (bidAmount != null) {
                    adSet.setBidAmount(Double.parseDouble(bidAmount.toString()) / 100);
                }

                // Extract age/gender from targeting
                Map targeting = (Map) as.get("targeting");
                if (targeting != null) {
                    adSet.setTargeting(targeting.toString());
                    Object ageMin = targeting.get("age_min");
                    Object ageMax = targeting.get("age_max");
                    if (ageMin != null) adSet.setMinAge(Integer.parseInt(ageMin.toString()));
                    if (ageMax != null) adSet.setMaxAge(Integer.parseInt(ageMax.toString()));

                    List genders = (List) targeting.get("genders");
                    if (genders != null) adSet.setGenders(genders.toString());
                }

                AdSet savedAdSet = adSetRepository.save(adSet);

                // Sync ads for this ad set
                syncAds(savedAdSet, accessToken);
            }
        } catch (Exception e) {
            System.out.println("Ad set sync failed for campaign: " + campaign.getName() + " - " + e.getMessage());
        }
    }

    private void syncAds(AdSet adSet, String accessToken) {
        try {
            Map response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.facebook.com")
                            .path("/v19.0/" + adSet.getPlatformAdSetId() + "/ads")
                            .queryParam("fields", "id,name,status,creative")
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            System.out.println("Ads response for adset " + adSet.getName() + ": " + response);
            if (response == null) return;
            List<Map> ads = (List<Map>) response.get("data");
            if (ads == null) return;

            for (Map a : ads) {
                String platformAdId = (String) a.get("id");

                Ad ad = adRepository
                        .findByPlatformAdId(platformAdId)
                        .orElse(Ad.builder()
                                .adSet(adSet)
                                .platformAdId(platformAdId)
                                .build());

                ad.setName((String) a.get("name"));
                ad.setStatus((String) a.get("status"));
                ad.setLastSyncedAt(LocalDateTime.now());

                // Extract creative data
                Map creative = (Map) a.get("creative");
                if (creative != null) {
                    ad.setCreativeId((String) creative.get("id"));
                    ad.setHeadline((String) creative.get("title"));
                    ad.setBody((String) creative.get("body"));
                    ad.setCtaType((String) creative.get("call_to_action_type"));
                    ad.setImageUrl((String) creative.get("image_url"));
                    ad.setVideoId((String) creative.get("video_id"));
                    ad.setLinkUrl((String) creative.get("link_url"));

                    if (creative.get("video_id") != null) {
                        ad.setCreativeFormat("VIDEO");
                    } else if (creative.get("image_url") != null) {
                        ad.setCreativeFormat("IMAGE");
                    } else {
                        ad.setCreativeFormat("OTHER");
                    }
                }

                adRepository.save(ad);
            }
        } catch (Exception e) {
            System.out.println("Ads sync failed for ad set: " + adSet.getName() + " - " + e.getMessage());
        }
    }

    private void syncInsights(AdAccount account, String accessToken, String accountId) {
        try {
            Map response = webClientBuilder.build()
                    .get()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/insights" +
                            "?fields=campaign_id,campaign_name,spend,impressions,clicks,ctr,cpc,cpm,reach,frequency,cost_per_result,conversions" +
                            "&level=campaign&date_preset=last_30d" +
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

                    // Extract conversions
                    List<Map> conversions = (List<Map>) insight.get("conversions");
                    if (conversions != null && !conversions.isEmpty()) {
                        Object value = conversions.get(0).get("value");
                        if (value != null) campaign.setConversions(parseLong(value));
                    }

                    campaignRepository.save(campaign);
                    checkAndAlert(campaign);
                });
            }
        } catch (Exception e) {
            System.out.println("Campaign insights sync failed: " + e.getMessage());
        }
    }

    private void syncAdSetInsights(AdAccount account, String accessToken, String accountId) {
        try {
            Map response = webClientBuilder.build()
                    .get()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/insights" +
                            "?fields=adset_id,adset_name,spend,impressions,clicks,ctr,cpc,cpm,reach,frequency,cost_per_result" +
                            "&level=adset&date_preset=last_30d" +
                            "&access_token=" + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return;
            List<Map> insights = (List<Map>) response.get("data");
            if (insights == null) return;

            for (Map insight : insights) {
                String platformAdSetId = (String) insight.get("adset_id");
                adSetRepository.findByPlatformAdSetId(platformAdSetId).ifPresent(adSet -> {
                    adSet.setSpend(parseDouble(insight.get("spend")));
                    adSet.setImpressions(parseLong(insight.get("impressions")));
                    adSet.setClicks(parseLong(insight.get("clicks")));
                    adSet.setCtr(parseDouble(insight.get("ctr")));
                    adSet.setCpc(parseDouble(insight.get("cpc")));
                    adSet.setCpm(parseDouble(insight.get("cpm")));
                    adSet.setLastSyncedAt(LocalDateTime.now());
                    adSetRepository.save(adSet);
                });
            }
        } catch (Exception e) {
            System.out.println("Ad set insights sync failed: " + e.getMessage());
        }
    }

    private void syncAdInsights(AdAccount account, String accessToken, String accountId) {
        try {
            Map response = webClientBuilder.build()
                    .get()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/insights" +
                            "?fields=ad_id,ad_name,spend,impressions,clicks,ctr,cpc,cpm,reach,frequency,cost_per_result" +
                            "&level=ad&date_preset=last_30d" +
                            "&access_token=" + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return;
            List<Map> insights = (List<Map>) response.get("data");
            if (insights == null) return;

            for (Map insight : insights) {
                String platformAdId = (String) insight.get("ad_id");
                adRepository.findByPlatformAdId(platformAdId).ifPresent(ad -> {
                    ad.setSpend(parseDouble(insight.get("spend")));
                    ad.setImpressions(parseLong(insight.get("impressions")));
                    ad.setClicks(parseLong(insight.get("clicks")));
                    ad.setCtr(parseDouble(insight.get("ctr")));
                    ad.setCpc(parseDouble(insight.get("cpc")));
                    ad.setCpm(parseDouble(insight.get("cpm")));
                    ad.setLastSyncedAt(LocalDateTime.now());
                    adRepository.save(ad);
                });
            }
        } catch (Exception e) {
            System.out.println("Ad insights sync failed: " + e.getMessage());
        }
    }

    private void checkAndAlert(Campaign campaign) {
        if (campaign.getCpc() == null) return;
        double targetCpl = 50.0;
        double currentCpl = campaign.getCpc();
        if (currentCpl > targetCpl * 1.5) {
            whatsAppService.sendCampaignAlert(campaign.getName(), currentCpl, targetCpl);
        }
    }

    @Scheduled(fixedDelay = 900000)
    public void scheduledSync() {
        System.out.println("Running scheduled campaign sync...");
        adAccountRepository.findAll()
                .stream()
                .map(account -> account.getUser().getId())
                .distinct()
                .forEach(userId -> {
                    try {
                        List<Campaign> campaigns = syncCampaigns(userId);
                        System.out.println("Synced campaigns for user: " + userId);
                        analyzeCampaignLearnings(userId, campaigns);
                    } catch (Exception e) {
                        System.out.println("Sync failed for user: " + userId + " - " + e.getMessage());
                    }
                });
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

    private void analyzeCampaignLearnings(String userId, List<Campaign> campaigns) {
        if (campaigns == null || campaigns.isEmpty()) return;

        try {
            String industry = "business";
            try {
                var business = businessRepository.findByUserId(userId);
                if (business.isPresent() && business.get().getIndustry() != null) {
                    industry = business.get().getIndustry();
                }
            } catch (Exception ignored) {}

            List<Map<String, Object>> campaignMaps = campaigns.stream().map(c -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("name", c.getName());
                map.put("status", c.getStatus());
                map.put("spend", c.getSpend() != null ? c.getSpend() : 0);
                map.put("clicks", c.getClicks() != null ? c.getClicks() : 0);
                map.put("ctr", c.getCtr() != null ? c.getCtr() : 0);
                map.put("cpc", c.getCpc() != null ? c.getCpc() : 0);
                map.put("daily_budget", c.getDailyBudget() != null ? c.getDailyBudget() : 0);
                map.put("impressions", c.getImpressions() != null ? c.getImpressions() : 0);
                return map;
            }).collect(java.util.stream.Collectors.toList());

            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("business_id", userId);
            payload.put("campaigns", campaignMaps);
            payload.put("industry", industry);

            webClientBuilder.build()
                    .post()
                    .uri("http://localhost:8001/analyze-campaign-learnings")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .subscribe(
                            result -> System.out.println("Campaign learnings updated for user: " + userId),
                            error -> System.out.println("Campaign learning failed: " + error.getMessage())
                    );
        } catch (Exception e) {
            System.out.println("Campaign learning error: " + e.getMessage());
        }
    }
}