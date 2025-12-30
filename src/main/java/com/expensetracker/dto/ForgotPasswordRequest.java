package com.expensetracker.dto;

import javax.validation.constraints.NotBlank;

public class ForgotPasswordRequest {
    
    @NotBlank(message = "Username or email is required")
    private String identifier; // Can be username or email
    
    public ForgotPasswordRequest() {}
    
    public ForgotPasswordRequest(String identifier) {
        this.identifier = identifier;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
