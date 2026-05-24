package com.adpilot.backend.waitlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/join")
    public ResponseEntity<Map<String, String>> join(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "invalid_email"));
        }

        String result = waitlistService.addToWaitlist(email.toLowerCase().trim());
        return ResponseEntity.ok(Map.of("status", result));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("count", waitlistService.getCount()));
    }
}