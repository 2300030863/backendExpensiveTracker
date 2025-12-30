package com.expensetracker.repository;

import com.expensetracker.entity.Admin;
import com.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    Optional<Admin> findByUsername(String username);
    
    Optional<Admin> findByEmail(String email);
    
    List<Admin> findByOwner(User owner);
    
    List<Admin> findByIsActive(Boolean isActive);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}
