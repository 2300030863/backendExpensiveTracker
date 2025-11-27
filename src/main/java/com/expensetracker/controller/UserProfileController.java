package com.expensetracker.controller;

import com.expensetracker.dto.ChangePasswordRequest;
import com.expensetracker.dto.UserProfileDto;
import com.expensetracker.dto.UserProfileRequest;
import com.expensetracker.entity.User;
import com.expensetracker.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/profile")
@CrossOrigin(origins = "*")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileDto> getUserProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        UserProfileDto profile = userProfileService.getUserProfile(user);
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<UserProfileDto> updateUserProfile(@Valid @RequestBody UserProfileRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        UserProfileDto updatedProfile = userProfileService.updateUserProfile(user, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        userProfileService.changePassword(user, request);
        return ResponseEntity.ok().build();
    }
}

