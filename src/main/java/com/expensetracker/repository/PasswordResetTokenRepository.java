package com.expensetracker.repository;

import com.expensetracker.entity.PasswordResetToken;
import com.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    void deleteByUserId(Long userId);
    
    Optional<PasswordResetToken> findByToken(String token);
    
    void deleteByUser(User user);
    
    void deleteByExpiryDateBefore(LocalDateTime now);
}
