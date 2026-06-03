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
    public Map<String, Object> createCampaign(String userId, Map<String, Object> campaignData) {
        List<AdAccount> accounts = adAccountRepository.findByUserId(userId);
        AdAccount adAccount = accounts.stream()
                .filter(a -> a.getPlatform().equals("META"))
                .filter(a -> a.getCurrency().equals("EGP"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No Meta EGP ad account found"));

        String accessToken = adAccount.getAccessToken();
        String accountId = adAccount.getPlatformAccountId();

        try {
            // Step 1 — Create campaign
            Map<String, Object> campaignPayload = new java.util.HashMap<>();
            campaignPayload.put("name", campaignData.get("name"));
            campaignPayload.put("objective", campaignData.get("objective"));
            campaignPayload.put("status", "PAUSED");
            campaignPayload.put("special_ad_categories", new java.util.ArrayList<>());
            campaignPayload.put("is_adset_budget_sharing_enabled", false);

            System.out.println("Creating campaign: " + campaignData.get("name"));

            Map campaignResponse = webClientBuilder.build()
                    .post()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/campaigns?access_token=" + accessToken)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(campaignPayload)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            response.bodyToMono(String.class).map(b -> {
                                System.out.println("Meta campaign error: " + b);
                                return new RuntimeException("Meta error: " + b);
                            })
                    )
                    .bodyToMono(Map.class)
                    .block();

            String platformCampaignId = (String) campaignResponse.get("id");
            System.out.println("Campaign created: " + platformCampaignId);

            // Step 2 — Create ad set
            Map<String, Object> adSetPayload = new java.util.HashMap<>();
            adSetPayload.put("name", campaignData.get("adSetName"));
            adSetPayload.put("campaign_id", platformCampaignId);
            adSetPayload.put("daily_budget", (int)(Double.parseDouble(campaignData.get("dailyBudget").toString()) * 100));
            adSetPayload.put("billing_event", "IMPRESSIONS");
            adSetPayload.put("optimization_goal", campaignData.get("optimizationGoal"));
            adSetPayload.put("bid_strategy", "LOWEST_COST_WITHOUT_CAP");
            adSetPayload.put("status", "PAUSED");

            String dynamicPageId = getPageId(accessToken, accountId);
            Map<String, Object> promotedObject = new java.util.HashMap<>();
            promotedObject.put("page_id", dynamicPageId);
            adSetPayload.put("promoted_object", promotedObject);

            // Build targeting from AI strategy
            Map<String, Object> cleanTargeting = new java.util.HashMap<>();
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map targetingMap = mapper.readValue(campaignData.get("targeting").toString(), Map.class);

                // Age
                Object ageMin = targetingMap.get("age_min");
                Object ageMax = targetingMap.get("age_max");
                cleanTargeting.put("age_min", ageMin != null ? Integer.parseInt(ageMin.toString()) : 18);
                cleanTargeting.put("age_max", ageMax != null ? Integer.parseInt(ageMax.toString()) : 65);

                // Gender
                Object genders = targetingMap.get("genders");
                if (genders != null) cleanTargeting.put("genders", genders);

                // Location — always use country level (city keys from GPT are unreliable)
                Map<String, Object> geoLocations = new java.util.HashMap<>();
                geoLocations.put("countries", java.util.List.of("EG"));
                cleanTargeting.put("geo_locations", geoLocations);

            } catch (Exception e) {
                System.out.println("Targeting parse error, using defaults: " + e.getMessage());
                cleanTargeting.put("age_min", 18);
                cleanTargeting.put("age_max", 65);
                Map<String, Object> geoLocations = new java.util.HashMap<>();
                geoLocations.put("countries", java.util.List.of("EG"));
                cleanTargeting.put("geo_locations", geoLocations);
            }
            Map<String, Object> targetingAutomation = new java.util.HashMap<>();
            targetingAutomation.put("advantage_audience", 0);
            cleanTargeting.put("targeting_automation", targetingAutomation);
            adSetPayload.put("targeting", cleanTargeting);

            System.out.println("Creating ad set with targeting: age " +
                    cleanTargeting.get("age_min") + "-" + cleanTargeting.get("age_max") +
                    " genders: " + cleanTargeting.get("genders"));

            Map adSetResponse = webClientBuilder.build()
                    .post()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/adsets?access_token=" + accessToken)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(adSetPayload)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            response.bodyToMono(String.class).map(b -> {
                                System.out.println("Meta adset error: " + b);
                                return new RuntimeException("Meta error: " + b);
                            })
                    )
                    .bodyToMono(Map.class)
                    .block();

            String platformAdSetId = (String) adSetResponse.get("id");
            System.out.println("Ad set created: " + platformAdSetId);

            // Step 3 — Upload all images and create ads
            String adId = null;
            List<String> imageHashes = new java.util.ArrayList<>();

            try {
                // Upload uploaded image files
                Object imagesObj = campaignData.get("images");
                if (imagesObj instanceof List) {
                    List<org.springframework.web.multipart.MultipartFile> imageFiles =
                            (List<org.springframework.web.multipart.MultipartFile>) imagesObj;
                    for (org.springframework.web.multipart.MultipartFile imageFile : imageFiles) {
                        if (imageFile != null && !imageFile.isEmpty()) {
                            String hash = uploadImageToMeta(accountId, accessToken, imageFile);
                            if (hash != null) imageHashes.add(hash);
                        }
                    }
                }

                // Upload AI generated image URLs
                String imageUrlsJson = (String) campaignData.get("imageUrls");
                if (imageUrlsJson != null && !imageUrlsJson.isEmpty()) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<String> imageUrls = mapper.readValue(imageUrlsJson, List.class);
                    for (String imageUrl : imageUrls) {
                        String hash = uploadImageUrlToMeta(accountId, accessToken, imageUrl);
                        if (hash != null) imageHashes.add(hash);
                    }
                }

                // Remove duplicate hashes
                imageHashes = imageHashes.stream().distinct().collect(java.util.stream.Collectors.toList());
                System.out.println("Total unique image hashes: " + imageHashes.size());

                if (!imageHashes.isEmpty()) {
                    String pageId = getPageId(accessToken , accountId);

                    // Parse copy variants
                    List<String> headlines = new java.util.ArrayList<>();
                    List<String> bodies = new java.util.ArrayList<>();
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

                    String headlinesJson = (String) campaignData.get("headlines");
                    String bodiesJson = (String) campaignData.get("bodies");

                    if (headlinesJson != null && !headlinesJson.isEmpty()) {
                        headlines = mapper.readValue(headlinesJson, List.class);
                    }
                    if (bodiesJson != null && !bodiesJson.isEmpty()) {
                        bodies = mapper.readValue(bodiesJson, List.class);
                    }

                    if (headlines.isEmpty()) headlines.add(campaignData.getOrDefault("headline", "Learn More").toString());
                    if (bodies.isEmpty()) bodies.add(campaignData.getOrDefault("body", "").toString());

                    String linkUrl = campaignData.getOrDefault("linkUrl", "https://adpilot-frontend-chi.vercel.app").toString();
                    String ctaType = campaignData.getOrDefault("cta", "LEARN_MORE").toString();

                    // Create one ad per image with rotated headline/body
                    List<String> createdAdIds = new java.util.ArrayList<>();

                    for (int i = 0; i < imageHashes.size(); i++) {
                        String adHeadline = headlines.get(i % headlines.size());
                        String adBody = bodies.get(i % bodies.size());

                        String creativeId = createSingleCreative(
                                accountId, accessToken, pageId,
                                imageHashes.get(i), adHeadline, adBody,
                                linkUrl, ctaType,
                                campaignData.get("name").toString() + " v" + (i + 1));

                        if (creativeId != null) {
                            Map<String, Object> adPayload = new java.util.HashMap<>();
                            adPayload.put("name", campaignData.get("name") + " Ad v" + (i + 1));
                            adPayload.put("adset_id", platformAdSetId);
                            adPayload.put("creative", java.util.Map.of("creative_id", creativeId));
                            adPayload.put("status", "PAUSED");

                            Map adResponse = webClientBuilder.build()
                                    .post()
                                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/ads?access_token=" + accessToken)
                                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                    .bodyValue(adPayload)
                                    .retrieve()
                                    .onStatus(status -> status.is4xxClientError(), response ->
                                            response.bodyToMono(String.class).map(b -> {
                                                System.out.println("Meta ad error: " + b);
                                                return new RuntimeException("Ad error: " + b);
                                            })
                                    )
                                    .bodyToMono(Map.class)
                                    .block();

                            String createdAdId = (String) adResponse.get("id");
                            if (createdAdId != null) {
                                createdAdIds.add(createdAdId);
                                System.out.println("Ad v" + (i + 1) + " created: " + createdAdId);
                            }
                        }
                    }

                    adId = createdAdIds.isEmpty() ? null : createdAdIds.get(0);
                    System.out.println("Total ads created: " + createdAdIds.size());
                }

            } catch (Exception e) {
                System.out.println("Creative/Ad creation failed (non-critical): " + e.getMessage());
            }

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", true);
            result.put("campaignId", platformCampaignId);
            result.put("adSetId", platformAdSetId);
            result.put("adId", adId);
            result.put("imageCount", imageHashes.size());
            result.put("status", "PAUSED");
            result.put("message", adId != null
                    ? "Campaign created with " + imageHashes.size() + " ad(s) — PAUSED, review and activate when ready"
                    : "Campaign and ad set created — add creative in Meta Ads Manager");
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create campaign: " + e.getMessage());
        }
    }

    // Helper — upload image file to Meta
    private String uploadImageToMeta(String accountId, String accessToken,
                                     org.springframework.web.multipart.MultipartFile imageFile) {
        try {
            org.springframework.core.io.ByteArrayResource imageResource =
                    new org.springframework.core.io.ByteArrayResource(imageFile.getBytes()) {
                        @Override public String getFilename() { return imageFile.getOriginalFilename(); }
                    };

            org.springframework.util.MultiValueMap<String, Object> payload =
                    new org.springframework.util.LinkedMultiValueMap<>();
            payload.add("filename", imageResource);
            payload.add("access_token", accessToken);

            Map response = webClientBuilder.build()
                    .post()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/adimages")
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("images") != null) {
                Map images = (Map) response.get("images");
                Map firstImage = (Map) images.values().iterator().next();
                String hash = (String) firstImage.get("hash");
                System.out.println("Image uploaded, hash: " + hash);
                return hash;
            }
        } catch (Exception e) {
            System.out.println("Image upload failed: " + e.getMessage());
        }
        return null;
    }

    // Helper — upload image URL to Meta
    private String uploadImageUrlToMeta(String accountId, String accessToken, String imageUrl) {
        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("url", imageUrl);
            payload.put("access_token", accessToken);

            Map response = webClientBuilder.build()
                    .post()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/adimages")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("images") != null) {
                Map images = (Map) response.get("images");
                Map firstImage = (Map) images.values().iterator().next();
                String hash = (String) firstImage.get("hash");
                System.out.println("Image URL uploaded, hash: " + hash);
                return hash;
            }
        } catch (Exception e) {
            System.out.println("Image URL upload failed: " + e.getMessage());
        }
        return null;
    }

    // Helper — get page ID
    private String getPageId(String accessToken, String accountId) {
        // First try to get from saved ad account
        try {
            AdAccount adAccount = adAccountRepository.findByPlatformAccountId(accountId);
            if (adAccount != null && adAccount.getPageId() != null) {
                System.out.println("Using saved page ID: " + adAccount.getPageId());
                return adAccount.getPageId();
            }
        } catch (Exception e) {
            System.out.println("Could not get page ID from account: " + e.getMessage());
        }

        // Fallback — fetch from Meta API
        try {
            Map pagesResponse = webClientBuilder.build()
                    .get()
                    .uri("https://graph.facebook.com/v19.0/me/accounts?access_token=" + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (pagesResponse != null && pagesResponse.get("data") != null) {
                List<Map> pages = (List<Map>) pagesResponse.get("data");
                if (!pages.isEmpty()) {
                    return (String) pages.get(0).get("id");
                }
            }
        } catch (Exception e) {
            System.out.println("Could not fetch pages from API: " + e.getMessage());
        }

        return "1108623479004638"; // last resort fallback
    }

    // Helper — create single image creative
    private String createSingleCreative(String accountId, String accessToken, String pageId,
                                        String imageHash, String headline, String body, String linkUrl, String cta, String name) {
        try {
            Map<String, Object> linkData = new java.util.HashMap<>();
            linkData.put("image_hash", imageHash);
            linkData.put("link", linkUrl);
            linkData.put("message", body);
            linkData.put("name", headline);

            Map<String, Object> callToAction = new java.util.HashMap<>();
            callToAction.put("type", cta);
            Map<String, Object> ctaValue = new java.util.HashMap<>();
            ctaValue.put("link", linkUrl);
            callToAction.put("value", ctaValue);
            linkData.put("call_to_action", callToAction);

            Map<String, Object> objectStorySpec = new java.util.HashMap<>();
            objectStorySpec.put("page_id", pageId);
            objectStorySpec.put("link_data", linkData);

            Map<String, Object> creativePayload = new java.util.HashMap<>();
            creativePayload.put("name", name + " Creative");
            creativePayload.put("object_story_spec", objectStorySpec);

            Map creativeResponse = webClientBuilder.build()
                    .post()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/adcreatives?access_token=" + accessToken)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(creativePayload)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            response.bodyToMono(String.class).map(b -> {
                                System.out.println("Meta creative error: " + b);
                                return new RuntimeException("Creative error: " + b);
                            })
                    )
                    .bodyToMono(Map.class)
                    .block();

            String creativeId = (String) creativeResponse.get("id");
            System.out.println("Single creative created: " + creativeId);
            return creativeId;
        } catch (Exception e) {
            System.out.println("Single creative failed: " + e.getMessage());
            return null;
        }
    }

    // Helper — create dynamic creative with multiple assets
    private String createDynamicCreative(String accountId, String accessToken, String pageId,
                                         List<String> imageHashes, List<String> headlines, List<String> bodies,
                                         String linkUrl, String cta, String name) {
        try {
            // Build asset feed spec
            Map<String, Object> assetFeedSpec = new java.util.HashMap<>();

            // Images
            List<Map<String, Object>> images = new java.util.ArrayList<>();
            for (String hash : imageHashes) {
                Map<String, Object> img = new java.util.HashMap<>();
                img.put("hash", hash);
                images.add(img);
            }
            assetFeedSpec.put("images", images);

            // Headlines
            List<Map<String, Object>> titlesList = new java.util.ArrayList<>();
            for (String h : headlines) {
                Map<String, Object> t = new java.util.HashMap<>();
                t.put("text", h);
                titlesList.add(t);
            }
            assetFeedSpec.put("titles", titlesList);

            // Bodies
            List<Map<String, Object>> bodiesList = new java.util.ArrayList<>();
            for (String b : bodies) {
                Map<String, Object> bodyMap = new java.util.HashMap<>();
                bodyMap.put("text", b);
                bodiesList.add(bodyMap);
            }
            assetFeedSpec.put("bodies", bodiesList);

            // Link URLs
            List<Map<String, Object>> linkUrls = new java.util.ArrayList<>();
            Map<String, Object> linkUrlMap = new java.util.HashMap<>();
            linkUrlMap.put("website_url", linkUrl);
            linkUrls.add(linkUrlMap);
            assetFeedSpec.put("link_urls", linkUrls);

            // CTA
            assetFeedSpec.put("call_to_action_types", java.util.List.of(cta));
            assetFeedSpec.put("ad_formats", java.util.List.of("SINGLE_IMAGE"));

            Map<String, Object> creativePayload = new java.util.HashMap<>();
            creativePayload.put("name", name + " Dynamic Creative");
            creativePayload.put("asset_feed_spec", assetFeedSpec);
            creativePayload.put("page_id", pageId);

            Map creativeResponse = webClientBuilder.build()
                    .post()
                    .uri("https://graph.facebook.com/v19.0/" + accountId + "/adcreatives?access_token=" + accessToken)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(creativePayload)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            response.bodyToMono(String.class).map(b -> {
                                System.out.println("Meta dynamic creative error: " + b);
                                return new RuntimeException("Dynamic creative error: " + b);
                            })
                    )
                    .bodyToMono(Map.class)
                    .block();

            String creativeId = (String) creativeResponse.get("id");
            System.out.println("Dynamic creative created: " + creativeId);
            return creativeId;
        } catch (Exception e) {
            System.out.println("Dynamic creative failed: " + e.getMessage());
            return null;
        }
    }
    }
