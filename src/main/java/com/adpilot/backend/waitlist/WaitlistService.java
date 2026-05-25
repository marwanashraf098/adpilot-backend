package com.adpilot.backend.waitlist;

import com.adpilot.backend.notification.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final WhatsAppService whatsAppService;

    public String addToWaitlist(String email) {
        if (waitlistRepository.existsByEmail(email)) {
            return "already_exists";
        }

        Waitlist entry = Waitlist.builder()
                .email(email)
                .build();

        waitlistRepository.save(entry);

        // Notify you via WhatsApp
        whatsAppService.sendWaitlistNotification(email);

        return "success";
    }

    public long getCount() {
        return waitlistRepository.count();
    }
}