package com.adpilot.backend.business;

import com.adpilot.backend.user.User;
import com.adpilot.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

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
        if (updates.containsKey("monthlyBudget")) business.setMonthlyBudget(((Number) updates.get("monthlyBudget")).doubleValue());
        if (updates.containsKey("targetCpl")) business.setTargetCpl(((Number) updates.get("targetCpl")).doubleValue());
        if (updates.containsKey("phoneNumber")) business.setPhoneNumber((String) updates.get("phoneNumber"));
        if (updates.containsKey("biggestChallenge")) business.setBiggestChallenge((String) updates.get("biggestChallenge"));
        if (updates.containsKey("competitors")) business.setCompetitors((String) updates.get("competitors"));
        if (updates.containsKey("competitorAdvantage")) business.setCompetitorAdvantage((String) updates.get("competitorAdvantage"));
        if (updates.containsKey("ourAdvantage")) business.setOurAdvantage((String) updates.get("ourAdvantage"));
        if (updates.containsKey("healthScore")) business.setHealthScore((Integer) updates.get("healthScore"));
        if (updates.containsKey("onboardingComplete")) business.setOnboardingComplete((Boolean) updates.get("onboardingComplete"));

        return businessRepository.save(business);
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