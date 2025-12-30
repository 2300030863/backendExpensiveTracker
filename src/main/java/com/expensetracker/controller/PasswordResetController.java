package com.expensetracker.controller;

import com.expensetracker.dto.ForgotPasswordRequest;
import com.expensetracker.dto.ResetPasswordRequest;
import com.expensetracker.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PasswordResetController {
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.createPasswordResetToken(request.getIdentifier());
            
            Map<String, String> response = new HashMap<>();
            // Generic message that doesn't reveal if account exists (security best practice)
            response.put("message", "If this account is registered, a password reset link has been sent to the registered email");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Even on error, return generic message to prevent account enumeration
            Map<String, String> response = new HashMap<>();
            response.put("message", "If this account is registered, a password reset link has been sent to the registered email");
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password has been reset successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
