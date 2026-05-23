package com.adpilot.backend.adaccount;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class MetaOAuthController {

    private final MetaOAuthService metaOAuthService;

    // Step 1: Frontend calls this to get the Facebook login URL
    @GetMapping("/oauth-url")
    public ResponseEntity<Map<String, String>> getOAuthUrl(@RequestParam String userId) {
        String url = metaOAuthService.generateOAuthUrl(userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // Step 2: Meta redirects here after user approves
    @GetMapping("/callback")
    public ResponseEntity<Map> handleCallback(
            @RequestParam String code,
            @RequestParam String state) {

        // state = userId we passed earlier
        String userId = state;

        // Exchange code for access token
        String accessToken = metaOAuthService.exchangeCodeForToken(code);

        // Get their ad accounts from Meta
        List<Map> adAccounts = metaOAuthService.getAdAccounts(accessToken);

        // Save the first ad account (we'll let them choose later)
        if (adAccounts != null && !adAccounts.isEmpty()) {
            Map firstAccount = adAccounts.get(0);
            String platformAccountId = (String) firstAccount.get("id");
            String accountName = (String) firstAccount.get("name");
            String currency = (String) firstAccount.get("currency");

            metaOAuthService.saveAdAccount(
                    userId,
                    accessToken,
                    platformAccountId,
                    accountName,
                    currency
            );
        }

        // Redirect to frontend dashboard
        return ResponseEntity.ok(Map.of(
                "message", "Account connected successfully",
                "accounts", adAccounts != null ? adAccounts : List.of()
        ));
    }

    // Get all connected accounts for a user
    @GetMapping("/accounts")
    public ResponseEntity<List<AdAccount>> getAccounts(@RequestParam String userId) {
        return ResponseEntity.ok(metaOAuthService.getUserAccounts(userId));
    }
}