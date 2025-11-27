package com.expensetracker.repository;

import com.expensetracker.entity.RecurringTransaction;
import com.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    
    List<RecurringTransaction> findByUserAndIsActiveTrueOrderByNextDueDateAsc(User user);
    
    List<RecurringTransaction> findByUserOrderByNextDueDateAsc(User user);
    
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.user = :user AND rt.isActive = true AND rt.nextDueDate <= :date AND (rt.endDate IS NULL OR rt.endDate >= :date)")
    List<RecurringTransaction> findDueRecurringTransactions(@Param("user") User user, @Param("date") LocalDate date);
}



