package com.adpilot.backend.waitlist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;

    public String addToWaitlist(String email) {
        if (waitlistRepository.existsByEmail(email)) {
            return "already_exists";
        }

        Waitlist entry = Waitlist.builder()
                .email(email)
                .build();

        waitlistRepository.save(entry);
        return "success";
    }

    public long getCount() {
        return waitlistRepository.count();
    }
}