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
    
    void deleteByUserId(Long userId);
    
    List<Budget> findByUserAndIsActiveTrueOrderByStartDateDesc(User user);
    
    List<Budget> findByUserOrderByStartDateDesc(User user);
    
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND :date BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsForDate(@Param("user") User user, @Param("date") LocalDate date);
    
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND b.category.id = :categoryId AND :date BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsForCategoryAndDate(@Param("user") User user, @Param("categoryId") Long categoryId, @Param("date") LocalDate date);
    
    @Query("SELECT b FROM Budget b WHERE b.user.userGroup.id = :groupId AND b.isActive = true ORDER BY b.startDate DESC")
    List<Budget> findByUserGroupAndIsActiveTrueOrderByStartDateDesc(@Param("groupId") Long groupId);
    
    @Query("SELECT b FROM Budget b WHERE b.user.userGroup.id = :groupId AND b.isActive = true AND :date BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsForGroupAndDate(@Param("groupId") Long groupId, @Param("date") LocalDate date);
    
    @Query("SELECT b FROM Budget b WHERE b.user.userGroup.id = :groupId AND b.isActive = true AND b.category.id = :categoryId AND :date BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsForGroupCategoryAndDate(@Param("groupId") Long groupId, @Param("categoryId") Long categoryId, @Param("date") LocalDate date);
}



