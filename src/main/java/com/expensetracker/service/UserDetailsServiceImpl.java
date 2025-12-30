package com.expensetracker.service;

import com.expensetracker.entity.User;
import com.expensetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
        
        // Check if user is blocked - only applies to regular users (ROLE_USER), not admins or owners
        if (user.isBlocked() && user.getRole() == com.expensetracker.entity.UserRole.ROLE_USER) {
            throw new DisabledException("Your account has been blocked by the administrator. Please contact support for assistance.");
        }

        return user;
    }
}



