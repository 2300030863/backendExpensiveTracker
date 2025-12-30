package com.expensetracker.config;

import com.expensetracker.entity.User;
import com.expensetracker.entity.UserRole;
import com.expensetracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Check if default user already exists
        if (!userRepository.existsByUsername("SIVESH")) {
            // Create default OWNER user
            User defaultUser = new User();
            defaultUser.setUsername("SIVESH");
            defaultUser.setEmail("siveshkommalapati167@gmail.com");
            defaultUser.setPassword(passwordEncoder.encode("Sivesh@167"));
            defaultUser.setFirstName("SIVESH");
            defaultUser.setLastName("KOMMALAPATI");
            defaultUser.setCountry("India");
            defaultUser.setRole(UserRole.ROLE_OWNER);
            
            userRepository.save(defaultUser);
            logger.info("✓ Default OWNER user created: SIVESH");
        } else {
            logger.info("✓ Default user already exists: SIVESH");
        }
    }
}
