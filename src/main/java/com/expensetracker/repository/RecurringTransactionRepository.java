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
    
    void deleteByUserId(Long userId);
    
    List<RecurringTransaction> findByUserAndIsActiveTrueOrderByNextDueDateAsc(User user);
    
    List<RecurringTransaction> findByUserOrderByNextDueDateAsc(User user);
    
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.user = :user AND rt.isActive = true AND rt.nextDueDate <= :date AND (rt.endDate IS NULL OR rt.endDate >= :date)")
    List<RecurringTransaction> findDueRecurringTransactions(@Param("user") User user, @Param("date") LocalDate date);
    
    // For users in admin groups
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.user.userGroup.id = :groupId ORDER BY rt.nextDueDate ASC")
    List<RecurringTransaction> findByUserGroupOrderByNextDueDateAsc(@Param("groupId") Long groupId);
    
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.user.userGroup.id = :groupId AND rt.isActive = true ORDER BY rt.nextDueDate ASC")
    List<RecurringTransaction> findByUserGroupAndIsActiveTrueOrderByNextDueDateAsc(@Param("groupId") Long groupId);
    
    // Get distinct users who have recurring transactions
    @Query("SELECT DISTINCT rt.user FROM RecurringTransaction rt")
    List<User> findDistinctUsers();
}



