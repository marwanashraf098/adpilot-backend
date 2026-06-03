package com.adpilot.backend.adaccount;

import com.adpilot.backend.user.User;
import com.adpilot.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetaOAuthService {

    private final AdAccountRepository adAccountRepository;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${meta.app.id}")
    private String appId;

    @Value("${meta.app.secret}")
    private String appSecret;

    @Value("${meta.redirect.uri}")
    private String redirectUri;

    // Step 1: Generate the Facebook OAuth URL
    public String generateOAuthUrl(String userId) {
        return "https://www.facebook.com/dialog/oauth?" +
                "client_id=" + appId +
                "&redirect_uri=" + redirectUri +
                "&state=" + userId +
                "&scope=ads_read,ads_management,business_management";
    }

    // Step 2: Exchange code for access token
    public String exchangeCodeForToken(String code) {
        WebClient client = webClientBuilder.build();

        Map response = client.get()
                .uri("https://graph.facebook.com/v19.0/oauth/access_token" +
                        "?client_id=" + appId +
                        "&redirect_uri=" + redirectUri +
                        "&client_secret=" + appSecret +
                        "&code=" + code)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }

    // Step 3: Get ad accounts from Meta
    public List<Map> getAdAccounts(String accessToken) {
        WebClient client = webClientBuilder.build();

        Map response = client.get()
                .uri("https://graph.facebook.com/v19.0/me/adaccounts" +
                        "?fields=id,name,currency,account_status" +
                        "&access_token=" + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (List<Map>) response.get("data");
    }

    // Step 4: Save connected account to database
    public AdAccount saveAdAccount(String userId, String accessToken,
                                   String platformAccountId, String accountName,
                                   String currency) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (adAccountRepository.existsByUserIdAndPlatformAccountId(userId, platformAccountId)) {
            throw new RuntimeException("Account already connected");
        }

        AdAccount account = AdAccount.builder()
                .user(user)
                .platform("META")
                .platformAccountId(platformAccountId)
                .accessToken(accessToken)
                .accountName(accountName)
                .currency(currency)
                .build();
        // Fetch and save Facebook page ID
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
                    account.setPageId((String) pages.get(0).get("id"));
                    System.out.println("Saved page ID: " + account.getPageId());
                }
            }
        } catch (Exception e) {
            System.out.println("Could not fetch page ID during OAuth: " + e.getMessage());
        }
        return adAccountRepository.save(account);
    }

    // Get all connected accounts for a user
    public List<AdAccount> getUserAccounts(String userId) {
        return adAccountRepository.findByUserId(userId);
    }
}