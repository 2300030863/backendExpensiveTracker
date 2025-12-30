package com.expensetracker.service;

import com.expensetracker.entity.User;
import com.expensetracker.entity.UserRole;
import com.expensetracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class EmailCommunicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailCommunicationService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.email.from:siveshkommalapati167@gmail.com}")
    private String fromEmail;
    
    /**
     * Send email to multiple users by their registered email addresses
     * @param recipientIds List of user IDs
     * @param subject Email subject
     * @param messageText Email message
     * @param senderName Name of the sender (Owner)
     * @return Number of emails sent successfully
     */
    public int sendEmailToUsers(List<Long> recipientIds, String subject, String messageText, String senderName) {
        int sentCount = 0;
        List<String> failedRecipients = new ArrayList<>();
        
        for (Long userId : recipientIds) {
            User user = userRepository.findById(userId).orElse(null);
            
            if (user != null && user.getEmail() != null) {
                try {
                    sendEmail(user, subject, messageText, senderName);
                    sentCount++;
                } catch (Exception e) {
                    failedRecipients.add(user.getEmail());
                    logger.error("Failed to send email to: {} - {}", user.getEmail(), e.getMessage());
                }
            }
        }
        
        if (!failedRecipients.isEmpty()) {
            logger.warn("Failed to send emails to: {}", String.join(", ", failedRecipients));
        }
        
        return sentCount;
    }
    
    private void sendEmail(User user, String subject, String messageText, String senderName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail()); // Send to registered email only
        message.setSubject(subject);
        message.setText(
            "Hello " + user.getFirstName() + ",\n\n" +
            messageText + "\n\n" +
            "---\n" +
            "This message was sent by: " + senderName + " (System Owner)\n" +
            "Expense Tracker System\n\n" +
            "This email was sent to your registered email address: " + user.getEmail()
        );
        
        mailSender.send(message);
    }
    
    /**
     * Send email to all users with specific role
     * @param role User role (ROLE_USER or ROLE_ADMIN)
     * @param subject Email subject
     * @param messageText Email message
     * @param senderName Name of the sender
     * @return Number of emails sent
     */
    public int sendEmailToAllByRole(String roleString, String subject, String messageText, String senderName) {
        UserRole role = UserRole.valueOf(roleString);
        List<User> users = userRepository.findByRole(role);
        int sentCount = 0;
        
        for (User user : users) {
            if (user.getEmail() != null) {
                try {
                    sendEmail(user, subject, messageText, senderName);
                    sentCount++;
                } catch (Exception e) {
                    System.err.println("Failed to send email to: " + user.getEmail() + " - " + e.getMessage());
                }
            }
        }
        
        return sentCount;
    }
}
