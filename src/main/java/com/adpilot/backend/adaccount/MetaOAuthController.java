package com.adpilot.backend.adaccount;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MetaOAuthController {

    private final MetaOAuthService metaOAuthService;

    // Step 1: Frontend calls this to get the Facebook login URL
    @GetMapping("/oauth-url")
    public ResponseEntity<Map<String, String>> getOAuthUrl(
            @RequestParam String userId,
            @RequestParam(required = false) String source) {
        String url = metaOAuthService.generateOAuthUrl(userId, source);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // Step 2: Meta redirects here after user approves
    @GetMapping("/callback")
    public ResponseEntity<Map> handleCallback(
            @RequestParam String code,
            @RequestParam String state) {

        // state format: "userId" or "userId:onboarding"
        String userId = state.contains(":") ? state.split(":")[0] : state;
        boolean fromOnboarding = state.contains(":onboarding");

        // Exchange code for access token
        String accessToken = metaOAuthService.exchangeCodeForToken(code);

        // Get their ad accounts from Meta
        List<Map> adAccounts = metaOAuthService.getAdAccounts(accessToken);

        // Save ALL ad accounts
        if (adAccounts != null && !adAccounts.isEmpty()) {
            for (Map account : adAccounts) {
                String platformAccountId = (String) account.get("id");
                String accountName = (String) account.get("name");
                String currency = (String) account.get("currency");

                try {
                    metaOAuthService.saveAdAccount(
                            userId,
                            accessToken,
                            platformAccountId,
                            accountName,
                            currency
                    );
                } catch (RuntimeException e) {
                    System.out.println("Skipping already connected account: " + platformAccountId);
                }
            }
        }

        // Redirect based on where OAuth was initiated
        String redirectUrl = fromOnboarding
                ? "http://localhost:5173/onboarding?connected=true"
                : "http://localhost:5173/dashboard?connected=true";

        return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
    }

    // Get all connected accounts for a user
    @GetMapping("/accounts")
    public ResponseEntity<List<AdAccount>> getAccounts(@RequestParam String userId) {
        return ResponseEntity.ok(metaOAuthService.getUserAccounts(userId));
    }
}