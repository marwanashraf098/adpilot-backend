package com.adpilot.backend.notification;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.from}")
    private String fromNumber;

    @Value("${twilio.whatsapp.to}")
    private String toNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    public void sendAlert(String messageBody) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toNumber),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();
            System.out.println("WhatsApp sent: " + message.getSid());
        } catch (Exception e) {
            System.out.println("WhatsApp failed: " + e.getMessage());
        }
    }

    public void sendCampaignAlert(String campaignName, double currentCpl, double targetCpl) {
        String message = "🔴 *AdPilot Alert*\n\n" +
                "Campaign: *" + campaignName + "*\n" +
                "Current CPL: *EGP " + String.format("%.0f", currentCpl) + "*\n" +
                "Target CPL: *EGP " + String.format("%.0f", targetCpl) + "*\n\n" +
                "Your CPL is " + String.format("%.1f", currentCpl / targetCpl) + "x your target.\n\n" +
                "👉 Open AdPilot to review and take action.";

        sendAlert(message);
    }

    public void sendWaitlistNotification(String email) {
        String message = "🎉 *New Waitlist Signup!*\n\n" +
                "Email: *" + email + "*\n\n" +
                "Someone just joined the AdPilot waitlist.";

        sendAlert(message);
    }
}