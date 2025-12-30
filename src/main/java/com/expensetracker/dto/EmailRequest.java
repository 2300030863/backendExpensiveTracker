package com.expensetracker.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class EmailRequest {
    
    @NotEmpty(message = "Recipient IDs are required")
    private List<Long> recipientIds; // List of user IDs to send email to
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    public EmailRequest() {}
    
    public EmailRequest(List<Long> recipientIds, String subject, String message) {
        this.recipientIds = recipientIds;
        this.subject = subject;
        this.message = message;
    }
    
    public List<Long> getRecipientIds() {
        return recipientIds;
    }
    
    public void setRecipientIds(List<Long> recipientIds) {
        this.recipientIds = recipientIds;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
