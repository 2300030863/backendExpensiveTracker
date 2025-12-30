package com.expensetracker.service;

import com.expensetracker.entity.PasswordResetToken;
import com.expensetracker.entity.User;
import com.expensetracker.repository.PasswordResetTokenRepository;
import com.expensetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${app.email.from:siveshkommalapati167@gmail.com}")
    private String fromEmail;
    
    private static final int EXPIRATION_HOURS = 24;
    
    public void createPasswordResetToken(String identifier) {
        // SECURITY: Find user by username OR email
        // The reset link will ONLY be sent to the email address stored in the database
        // from when the account was created (user.getEmail())
        User user = null;
        
        // Try to find by email first
        user = userRepository.findByEmail(identifier).orElse(null);
        
        // If not found by email, try to find by username
        if (user == null) {
            user = userRepository.findByUsername(identifier).orElse(null);
        }
        
        if (user != null) {
            // Delete any existing tokens for this user
            tokenRepository.deleteByUser(user);
            
            // Generate new token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(EXPIRATION_HOURS);
            
            PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
            tokenRepository.save(resetToken);
            
            // CRITICAL: Send email ONLY to user.getEmail() - the registered email from database
            // User entered username or email, but reset link ALWAYS goes to registered email
            sendPasswordResetEmail(user, token);
        }
        // If user doesn't exist, silently succeed (security best practice)
        // This prevents attackers from discovering which accounts are registered
    }
    
    private void sendPasswordResetEmail(User user, String token) {
        String resetUrl = "http://localhost:5173/reset-password?token=" + token;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        // CRITICAL SECURITY: Email is sent ONLY to user.getEmail() which is the 
        // email address stored in the database when the account was created.
        // This value comes from the User entity, NOT from any user input.
        message.setTo(user.getEmail()); 
        message.setSubject("Password Reset Request - Expense Tracker");
        message.setText("Hello " + user.getFirstName() + ",\n\n" +
                "You have requested to reset your password for Expense Tracker.\n\n" +
                "Click the link below to reset your password:\n" +
                resetUrl + "\n\n" +
                "This link will expire in " + EXPIRATION_HOURS + " hours.\n\n" +
                "SECURITY NOTICE: This email was sent to your registered email address only. " +
                "If you did not request this password reset, please ignore this email and " +
                "consider changing your password immediately.\n\n" +
                "Best regards,\n" +
                "Expense Tracker Team");
        
        mailSender.send(message);
    }
    
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));
        
        if (resetToken.isUsed()) {
            throw new RuntimeException("This password reset token has already been used");
        }
        
        if (resetToken.isExpired()) {
            throw new RuntimeException("This password reset token has expired");
        }
        
        // Fetch user directly from repository to avoid lazy loading issues
        Long userId = resetToken.getUser().getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Encode and update password
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        
        // Save user first to ensure password is updated
        userRepository.save(user);
        userRepository.flush(); // Force immediate database update
        
        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
    
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
