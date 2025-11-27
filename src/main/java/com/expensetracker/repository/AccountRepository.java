package com.expensetracker.repository;

import com.expensetracker.entity.Account;
import com.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    List<Account> findByUserAndIsActiveTrueOrderByName(User user);
    
    List<Account> findByUserOrderByName(User user);
}



