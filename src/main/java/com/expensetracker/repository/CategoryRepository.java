package com.expensetracker.repository;

import com.expensetracker.entity.Admin;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    void deleteByUserId(Long userId);

    @Query("SELECT c FROM Category c WHERE c.user = :user OR c.isDefault = true ORDER BY c.name")
    List<Category> findByUserOrIsDefaultTrueOrderByName(@Param("user") User user);

    boolean existsByNameAndUser(String name, User user);
    
    List<Category> findByAdmin(Admin admin);
    
    // For users in admin groups - get categories from all users in the same group
    @Query("SELECT c FROM Category c WHERE c.user.userGroup.id = :groupId OR c.isDefault = true ORDER BY c.name")
    List<Category> findByUserGroupOrIsDefaultTrue(@Param("groupId") Long groupId);
    
    // Get all default categories
    @Query("SELECT c FROM Category c WHERE c.isDefault = true ORDER BY c.name")
    List<Category> findAllDefaultCategories();
    
    // Get categories by user ID
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId ORDER BY c.name")
    List<Category> findByUserId(@Param("userId") Long userId);
    
    // Get categories for admin's managed users
    @Query("SELECT DISTINCT c FROM Category c WHERE c.user.admin.id = :adminId OR (c.user.userGroup.admin.id = :adminId) ORDER BY c.name")
    List<Category> findByAdminManagedUsers(@Param("adminId") Long adminId);
    
    // Get categories for group members
    @Query("SELECT DISTINCT c FROM Category c WHERE c.user.userGroup.id = :groupId ORDER BY c.name")
    List<Category> findByGroupMembers(@Param("groupId") Long groupId);
}


