package com.adpilot.backend.auth;

import com.adpilot.backend.user.User;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String businessName;
    private User.Industry industry;
}