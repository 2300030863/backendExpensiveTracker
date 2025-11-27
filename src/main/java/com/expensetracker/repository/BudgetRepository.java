package com.expensetracker.repository;

import com.expensetracker.entity.Budget;
import com.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    List<Budget> findByUserAndIsActiveTrueOrderByStartDateDesc(User user);
    
    List<Budget> findByUserOrderByStartDateDesc(User user);
    
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND :date BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsForDate(@Param("user") User user, @Param("date") LocalDate date);
    
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND b.category.id = :categoryId AND :date BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsForCategoryAndDate(@Param("user") User user, @Param("categoryId") Long categoryId, @Param("date") LocalDate date);
}



