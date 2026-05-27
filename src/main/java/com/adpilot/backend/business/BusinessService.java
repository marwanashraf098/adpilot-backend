package com.adpilot.backend.business;

import com.adpilot.backend.user.User;
import com.adpilot.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;

    public Business getOrCreateBusiness(String userId) {
        return businessRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Business business = Business.builder()
                    .user(user)
                    .onboardingComplete(false)
                    .build();
            return businessRepository.save(business);
        });
    }

    public Business updateBusiness(String userId, Map<String, Object> updates) {
        Business business = getOrCreateBusiness(userId);

        if (updates.containsKey("websiteUrl")) business.setWebsiteUrl((String) updates.get("websiteUrl"));
        if (updates.containsKey("facebookPageUrl")) business.setFacebookPageUrl((String) updates.get("facebookPageUrl"));
        if (updates.containsKey("instagramUrl")) business.setInstagramUrl((String) updates.get("instagramUrl"));
        if (updates.containsKey("tiktokUrl")) business.setTiktokUrl((String) updates.get("tiktokUrl"));
        if (updates.containsKey("businessName")) business.setBusinessName((String) updates.get("businessName"));
        if (updates.containsKey("industry")) business.setIndustry((String) updates.get("industry"));
        if (updates.containsKey("city")) business.setCity((String) updates.get("city"));
        if (updates.containsKey("description")) business.setDescription((String) updates.get("description"));
        if (updates.containsKey("services")) business.setServices((String) updates.get("services"));
        if (updates.containsKey("uniqueSellingPoint")) business.setUniqueSellingPoint((String) updates.get("uniqueSellingPoint"));
        if (updates.containsKey("priceRange")) business.setPriceRange((String) updates.get("priceRange"));
        if (updates.containsKey("brandTone")) business.setBrandTone((String) updates.get("brandTone"));
        if (updates.containsKey("targetAudience")) business.setTargetAudience((String) updates.get("targetAudience"));
        if (updates.containsKey("minAge")) business.setMinAge((Integer) updates.get("minAge"));
        if (updates.containsKey("maxAge")) business.setMaxAge((Integer) updates.get("maxAge"));
        if (updates.containsKey("gender")) business.setGender((String) updates.get("gender"));
        if (updates.containsKey("customerSource")) business.setCustomerSource((String) updates.get("customerSource"));
        if (updates.containsKey("averageCustomerValue")) business.setAverageCustomerValue(((Number) updates.get("averageCustomerValue")).doubleValue());
        if (updates.containsKey("buyingCycle")) business.setBuyingCycle((String) updates.get("buyingCycle"));
        if (updates.containsKey("mainGoal")) business.setMainGoal((String) updates.get("mainGoal"));
        if (updates.containsKey("monthlyBudget")) business.setMonthlyBudget((String) updates.get("monthlyBudget"));
        if (updates.containsKey("targetCpl")) business.setTargetCpl(((Number) updates.get("targetCpl")).doubleValue());
        if (updates.containsKey("phoneNumber")) business.setPhoneNumber((String) updates.get("phoneNumber"));
        if (updates.containsKey("biggestChallenge")) business.setBiggestChallenge((String) updates.get("biggestChallenge"));
        if (updates.containsKey("competitors")) business.setCompetitors((String) updates.get("competitors"));
        if (updates.containsKey("competitorAdvantage")) business.setCompetitorAdvantage((String) updates.get("competitorAdvantage"));
        if (updates.containsKey("ourAdvantage")) business.setOurAdvantage((String) updates.get("ourAdvantage"));
        if (updates.containsKey("healthScore")) business.setHealthScore((Integer) updates.get("healthScore"));

        if (updates.containsKey("onboardingComplete") && Boolean.TRUE.equals(updates.get("onboardingComplete"))) {
            business.setOnboardingComplete(true);
            businessRepository.save(business);
            buildBusinessRag(userId);
            return business;
        }

        return businessRepository.save(business);
    }

    public void buildBusinessRag(String userId) {
        getBusiness(userId).ifPresent(business -> {
            try {
                Map<String, Object> businessData = new HashMap<>();
                businessData.put("businessName", business.getBusinessName());
                businessData.put("industry", business.getIndustry());
                businessData.put("city", business.getCity());
                businessData.put("description", business.getDescription());
                businessData.put("services", business.getServices());
                businessData.put("uniqueSellingPoint", business.getUniqueSellingPoint());
                businessData.put("priceRange", business.getPriceRange());
                businessData.put("brandTone", business.getBrandTone());
                businessData.put("targetAudience", business.getTargetAudience());
                businessData.put("minAge", business.getMinAge());
                businessData.put("maxAge", business.getMaxAge());
                businessData.put("gender", business.getGender());
                businessData.put("customerSource", business.getCustomerSource());
                businessData.put("buyingCycle", business.getBuyingCycle());
                businessData.put("averageCustomerValue", business.getAverageCustomerValue());
                businessData.put("mainGoal", business.getMainGoal());
                businessData.put("monthlyBudget", business.getMonthlyBudget());
                businessData.put("targetCpl", business.getTargetCpl());
                businessData.put("biggestChallenge", business.getBiggestChallenge());
                businessData.put("competitors", business.getCompetitors());
                businessData.put("competitorAdvantage", business.getCompetitorAdvantage());
                businessData.put("ourAdvantage", business.getOurAdvantage());
                businessData.put("websiteUrl", business.getWebsiteUrl());
                businessData.put("facebookPageUrl", business.getFacebookPageUrl());
                businessData.put("instagramUrl", business.getInstagramUrl());

                Map<String, Object> ragRequest = new HashMap<>();
                ragRequest.put("business_id", userId);
                ragRequest.put("business_data", businessData);

                webClientBuilder.build()
                        .post()
                        .uri("http://localhost:8001/build-business-rag")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .bodyValue(ragRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .subscribe(
                                result -> System.out.println("Business RAG built for user: " + userId),
                                error -> System.out.println("Business RAG build failed: " + error.getMessage())
                        );
            } catch (Exception e) {
                System.out.println("Business RAG build error: " + e.getMessage());
            }
        });
    }

    public Optional<Business> getBusiness(String userId) {
        return businessRepository.findByUserId(userId);
    }

    public boolean isOnboardingComplete(String userId) {
        return businessRepository.findByUserId(userId)
                .map(Business::getOnboardingComplete)
                .orElse(false);
    }
}